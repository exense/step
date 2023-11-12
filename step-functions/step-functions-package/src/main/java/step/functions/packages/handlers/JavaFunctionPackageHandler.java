package step.functions.packages.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.processes.ExternalJVMLauncher;
import ch.exense.commons.processes.ManagedProcess;
import step.attachments.FileResolver;
import step.automation.packages.AutomationPackageArchive;
import step.automation.packages.AutomationPackageKeywordsAttributesApplier;
import step.automation.packages.model.AutomationPackageKeyword;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectEnricher;
import step.functions.Function;
import step.functions.packages.FunctionPackage;
import step.plugins.java.GeneralScriptFunction;
import step.resources.ResourceManager;

public class JavaFunctionPackageHandler extends AbstractFunctionPackageHandler {

	private File processLogFolder;
	private String javaPath;

	protected AutomationPackageKeywordsAttributesApplier automationPackageKeywordsAttributesApplier;
	
	public JavaFunctionPackageHandler(FileResolver fileResolver, Configuration config, ResourceManager resourceManager) {
		super(fileResolver);

		this.automationPackageKeywordsAttributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);

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
			return getFunctionsFromDaemon(functionPackage, process, preview);
		}
	}

	@Override
	protected void configureFunction(Function f, FunctionPackage functionPackage, boolean preview, Map<String, Object> specialAutomationPackageAttributes, AutomationPackageArchive automationPackageArchive) {
		if (f instanceof GeneralScriptFunction) {
			GeneralScriptFunction function = (GeneralScriptFunction) f;

			function.setScriptLanguage(new DynamicValue<>("java"));
			function.setScriptFile(new DynamicValue<>(functionPackage.getPackageLocation()));
			function.setLibrariesFile(new DynamicValue<>(functionPackage.getPackageLibrariesLocation()));
		}

		// for automation packages some attributes are not applied during automation package processing (parsing)
		// here we need to apply them (for instance, save the jmeterTestPlan resource and link this resource with keyword)
		if (!preview && automationPackageArchive != null) {
			if (specialAutomationPackageAttributes != null && !specialAutomationPackageAttributes.isEmpty()) {
				AutomationPackageKeyword automationPackageKeyword = new AutomationPackageKeyword(f, specialAutomationPackageAttributes);
				automationPackageKeywordsAttributesApplier.applySpecialAttributesToKeyword(automationPackageKeyword, automationPackageArchive);
			}
		}
	}

	@Override
	public boolean isValidForPackage(FunctionPackage functionPackage) {
		File file = resolveFile(functionPackage.getPackageLocation());
		String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
		return extension.equals("jar");
	}
}
