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

/**
 * This class aims to import a set of {@link FunctionPackage} from the 
 * file system 
 *
 */
public class EmbeddedFunctionPackageImporter {

	private static final String PACKAGE_TRACKING_FIELD = "embeddedPackage";

	private static final String META_FILE_EXTENSION = ".json";

	private static final Logger logger = LoggerFactory.getLogger(EmbeddedFunctionPackageImporter.class);
	
	private final FunctionPackageAccessor functionPackageAccessor;
	private final FunctionPackageManager functionPackageManager;
	
	public EmbeddedFunctionPackageImporter(FunctionPackageAccessor functionPackageAccessor,
			FunctionPackageManager functionPackageManager) {
		super();
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

				try {
					FunctionPackage functionPackage = searchExistingFunctionPackage(f);
					addAttributeFromMetaFileIfAny(packageLocation, functionPackage);
					
					importedFunctionPackageIds.add(functionPackage.getId().toString());

					functionPackage.addCustomField(PACKAGE_TRACKING_FIELD, f);
					functionPackage.setExecuteLocally(localExecution);
					
					functionPackageManager.addOrUpdateFunctionPackage(functionPackage);
				} catch (Exception e) {
					logger.error("Error while importing function package " + f.getAbsolutePath(), e);
				}
			});
		} else {
			logger.info("The folder "+embeddedPackageFolderFile.getAbsolutePath()+" doesn't exist");
		}
		return importedFunctionPackageIds;
	}

	private FunctionPackage searchExistingFunctionPackage(File f) throws Exception {
		String packageLocation = f.getAbsolutePath();
		FunctionPackage existingFunctionPackage = StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(functionPackageAccessor.getAll(), Spliterator.ORDERED),
						false)
				.filter(p -> {
					if(f.getName().equals(p.getCustomField(PACKAGE_TRACKING_FIELD))) {
						return true;
					} else {
						// Support for versions < 3.18
						String name = new File(p.getPackageLocation()).getName();
						return f.getName().equals(name);
					}
				}).findFirst().orElse(null);
		if (existingFunctionPackage == null) {
			logger.info("Function package "+packageLocation+" doesn't exist. Creating it...");
			return newFunctionPackage(packageLocation);
		} else {
			logger.info("Function package "+packageLocation+" already exists. Updating it...");
			return existingFunctionPackage;
		}
	}

	private void addAttributeFromMetaFileIfAny(String packageLocation, FunctionPackage functionPackage)
			throws Exception {
		String metaFileName = packageLocation + META_FILE_EXTENSION;
		File metaFile = new File(metaFileName);
		if(metaFile.exists()) {
			try {
				FunctionPackage metaFunctionPackage = new ObjectMapper().readValue(metaFile, FunctionPackage.class);
				// Add all attributes defined in the meta file to the function package
				metaFunctionPackage.getAttributes().forEach((key, value) -> functionPackage.addAttribute(key, value));
			} catch (IOException e) {
				throw new Exception("Error while reading meta file for package "+packageLocation, e);
			}
		}
	}

	private FunctionPackage newFunctionPackage(String packageLocation) throws Exception {
		FunctionPackage newFunctionPackage = new FunctionPackage();
		newFunctionPackage.setPackageLocation(packageLocation);
		return newFunctionPackage;
	}
}
