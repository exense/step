package step.functions.packages.handlers;

import ch.exense.commons.processes.ManagedProcess;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.automation.packages.AutomationPackageFile;
import step.automation.packages.AutomationPackageReadingException;
import step.core.objectenricher.ObjectEnricher;
import step.functions.Function;
import step.functions.packages.FunctionPackage;
import step.functions.packages.FunctionPackageHandler;

import java.io.*;
import java.util.List;
import java.util.Map;

public abstract class AbstractFunctionPackageHandler extends FunctionPackageUtils implements FunctionPackageHandler {

	private static final Logger logger = LoggerFactory.getLogger(AbstractFunctionPackageHandler.class);

	public AbstractFunctionPackageHandler(FileResolver fileResolver) {
		super(fileResolver);
	}

	@Override
	public abstract boolean isValidForPackage(FunctionPackage functionPackage);
	
	@Override
	public abstract List<Function> buildFunctions(FunctionPackage functionPackage, boolean preview, ObjectEnricher objectEnricher) throws Exception;

	protected List<Function> getFunctionsFromDaemon(FunctionPackage functionPackage, ManagedProcess discovererDeamon, boolean preview)
			throws Exception {

		File packageFile = resolveMandatoryFile(functionPackage.getPackageLocation());
		File packageLibraryFile = resolveFile(functionPackage.getPackageLibrariesLocation());

		DiscovererParameters param = new DiscovererParameters();
		if (packageFile != null) {
			param.packageLocation = packageFile.getAbsolutePath();
		} else {
			throw new FileNotFoundException("The package location doesn't exist");
		}
		if (packageLibraryFile != null) {
			param.packageLibrariesLocation = packageLibraryFile.getAbsolutePath();
		} else {
			param.packageLibrariesLocation = "";
		}

		ObjectMapperResolver resolver = new ObjectMapperResolver();
		ObjectMapper objectMapper = resolver.getContext(FunctionList.class);

		try (OutputStream outputStream = discovererDeamon.getProcessOutputStream()) {
			String serializedRequest = objectMapper.writeValueAsString(param);
			outputStream.write(serializedRequest.getBytes());
		}

		FunctionList list;
		try (BufferedReader inputStream = new BufferedReader(
				new InputStreamReader(discovererDeamon.getProcessInputStream()))) {
			String res;
			do {
				res = inputStream.readLine();
				if (res==null) {
					logger.error("Unexpected error when starting the function package handler: '"+discovererDeamon.getProcessErrorLogAsString()+"'");
					throw new Exception("Unexpected error when starting the function package handler: the process exited before returning the result. See the logs on the controller for more detail");
				}
			} while (!res.equals(READY_STRING));
			list = objectMapper.readValue(inputStream, FunctionList.class);
		}

		if (list.exception == null) {
			List<Function> functions = list.getFunctions();
			functions.forEach(f -> {
				AutomationPackageFile automationPackageFile = null;
				if (list.getAutomationPackageAttributes().get(f.getId().toString()) != null) {
					// the function was included in automation package
					try {
						automationPackageFile = new AutomationPackageFile(packageFile);
					} catch (AutomationPackageReadingException e) {
						throw new RuntimeException(e);
					}
				}
				configureFunction(f, functionPackage, preview, list.getAutomationPackageAttributes().get(f.getId().toString()), automationPackageFile);
			});
			return list.getFunctions();
		} else {
			throw new Exception(list.exception);
		}
	}

	protected abstract void configureFunction(Function f, FunctionPackage functionPackage,
											  boolean preview, Map<String, Object> specialAutomationPackageAttributes,
											  AutomationPackageFile automationPackageFile);
}
