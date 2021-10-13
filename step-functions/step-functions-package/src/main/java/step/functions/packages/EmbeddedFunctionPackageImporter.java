package step.functions.packages;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.functions.Function;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;

/**
 * This class aims to import a set of {@link FunctionPackage} from the 
 * file system 
 *
 */
public class EmbeddedFunctionPackageImporter {

	private static final String META_FILE_EXTENSION = ".json";

	private static final Logger logger = LoggerFactory.getLogger(EmbeddedFunctionPackageImporter.class);
	
	private final FunctionManager functionManager;
	private final FunctionPackageAccessor functionPackageAccessor;
	private final FunctionPackageManager functionPackageManager;
	
	public EmbeddedFunctionPackageImporter(FunctionManager functionManager,
			FunctionPackageAccessor functionPackageAccessor, FunctionPackageManager functionPackageManager) {
		super();
		this.functionManager = functionManager;
		this.functionPackageAccessor = functionPackageAccessor;
		this.functionPackageManager = functionPackageManager;
	}

	/**
	 * Imports all the {@link FunctionPackage} from the specified folder. 
	 * The import will search in the 2 following sub folders for {@link FunctionPackage} 
	 * to be imported:
	 * <li> {packageFolder}/local/* {@link FunctionPackage} located in this folder will be imported as local function </li> 
	 * <li> {packageFolder}/remote/* {@link FunctionPackage} located in this folder will be imported as remote function </li>
	 * 
	 * @param packageFolder the folder containing the {@link FunctionPackage} to be imported
	 * @return the list of imported/updated {@link FunctionPackage} IDs
	 */
	public List<String> importEmbeddedFunctionPackages(@NotNull String packageFolder) {
		assert packageFolder != null;
		List<String> importedFunctionPackageIds = new ArrayList<>();
		importedFunctionPackageIds.addAll(importFunctionPackages(packageFolder+"/local", true));
		importedFunctionPackageIds.addAll(importFunctionPackages(packageFolder+"/remote", false));
		return importedFunctionPackageIds;
	}

	protected List<String> importFunctionPackages(String embeddedPackageFolder, boolean localExecution) {
		List<String> importedFunctionPackageIds = new ArrayList<>();
		File embeddedPackageFolderFile = new File(embeddedPackageFolder);
		if (embeddedPackageFolderFile.exists()) {
			logger.info("Importing function packages from folder "+embeddedPackageFolder);
			Arrays.asList(embeddedPackageFolderFile.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return ! name.endsWith(META_FILE_EXTENSION);
				}
			})).forEach(f -> {
				String packageLocation = f.getAbsolutePath();

				// Search a function package with the same package location
				FunctionPackage existingPackage = StreamSupport
						.stream(Spliterators.spliteratorUnknownSize(functionPackageAccessor.getAll(), Spliterator.ORDERED),
								false)
						.filter(p -> {
							return packageLocation.equals(p.getPackageLocation());
						}).findFirst().orElse(null);

				try {
					// Create a new function package if it doesn't already exist
					if (existingPackage == null) {
						logger.info("Function package "+packageLocation+" doesn't exist. Creating it...");
						existingPackage = newFunctionPackage(packageLocation);
					} else {
						logger.info("Function package "+packageLocation+" already exists. Updating it...");
					}
					
					importedFunctionPackageIds.add(existingPackage.getId().toString());
					
					existingPackage = functionPackageManager.addOrUpdateFunctionPackage(existingPackage, null);
					// Set the executeLocally flag of the imported functions accordingly
					existingPackage.getFunctions().forEach(id -> {
						Function function = functionManager.getFunctionById(id.toString());
						function.setExecuteLocally(localExecution);
						try {
							functionManager.saveFunction(function);
						} catch (SetupFunctionException | FunctionTypeException e) {
							logger.error("Error while saving function " + id, e);
						}
					});
				} catch (Exception e) {
					logger.error("Error while importing function package " + f.getAbsolutePath(), e);
				}
			});
		} else {
			logger.info("The folder "+embeddedPackageFolderFile.getAbsolutePath()+" doesn't exist");
		}
		return importedFunctionPackageIds;
	}

	private FunctionPackage newFunctionPackage(String packageLocation) throws Exception {
		FunctionPackage newFunctionPackage = new FunctionPackage();
		newFunctionPackage.setPackageLocation(packageLocation);
		
		String metaFileName = packageLocation + META_FILE_EXTENSION;
		File metaFile = new File(metaFileName);
		if(metaFile.exists()) {
			try {
				FunctionPackage functionPackage = new ObjectMapper().readValue(metaFile, FunctionPackage.class);
				// Add all attributes defined in the meta file to the function package
				functionPackage.getAttributes().forEach((key, value) -> newFunctionPackage.addAttribute(key, value));
			} catch (IOException e) {
				throw new Exception("Error while reading meta file for package "+packageLocation, e);
			}
		}
		return newFunctionPackage;
	}
}
