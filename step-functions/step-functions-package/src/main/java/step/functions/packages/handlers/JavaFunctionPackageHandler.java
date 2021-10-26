package step.functions.packages.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.processes.ExternalJVMLauncher;
import ch.exense.commons.processes.ManagedProcess;
import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectEnricher;
import step.functions.Function;
import step.functions.packages.FunctionPackage;
import step.plugins.java.GeneralScriptFunction;

public class JavaFunctionPackageHandler extends AbstractFunctionPackageHandler {

	private File processLogFolder;
	private String javaPath;
	
	public JavaFunctionPackageHandler(FileResolver fileResolver, Configuration config) {
		super(fileResolver);
		
		javaPath = System.getProperty("java.home")+"/bin/java";
		
		String logs = "../log/functionDiscoverer_java";
		processLogFolder = new File(logs);
	}
	
	@Override
	public List<Function> buildFunctions(FunctionPackage functionPackage, boolean preview, ObjectEnricher objectEnricher) throws Exception {
		ExternalJVMLauncher launcher = new ExternalJVMLauncher(javaPath, processLogFolder);
		
		List<String> vmargs = new ArrayList<>();
		List<String> progargs = new ArrayList<>();
		
		try (ManagedProcess process = launcher.launchExternalJVM("Java Function Discoverer", JavaFunctionPackageDaemon.class, vmargs, progargs, false)){
			return getFunctionsFromDaemon(functionPackage, process);
		}
	}

	@Override
	protected void configureFunction(Function f, FunctionPackage functionPackage) {
		GeneralScriptFunction function = (GeneralScriptFunction) f;
		
		function.setScriptLanguage(new DynamicValue<>("java"));
		function.setScriptFile(new DynamicValue<>(functionPackage.getPackageLocation()));
		function.setLibrariesFile(new DynamicValue<>(functionPackage.getPackageLibrariesLocation()));
	}

	@Override
	public boolean isValidForPackage(FunctionPackage functionPackage) {
		File file = resolveFile(functionPackage.getPackageLocation());
		String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
		return extension.equals("jar");
	}
}
