package step.functions.packages.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.exense.commons.processes.ManagedProcess;
import step.attachments.FileResolver;
import step.core.objectenricher.ObjectEnricher;
import step.functions.Function;
import step.functions.packages.FunctionPackage;
import step.functions.packages.FunctionPackageHandler;

public abstract class AbstractFunctionPackageHandler extends FunctionPackageUtils implements FunctionPackageHandler {

	public AbstractFunctionPackageHandler(FileResolver fileResolver) {
		super(fileResolver);
	}

	@Override
	public abstract boolean isValidForPackage(FunctionPackage functionPackage);
	
	@Override
	public abstract List<Function> buildFunctions(FunctionPackage functionPackage, boolean preview, ObjectEnricher objectEnricher) throws Exception;

	protected List<Function> getFunctionsFromDaemon(FunctionPackage functionPackage, ManagedProcess discovererDeamon)
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
					throw new Exception("Unexpected error when starting the function package handler: the process exited before returning the result. Error was: '"+
							discovererDeamon.getProcessErrorLogAsString()+"'");
				}
			} while (!res.equals(READY_STRING));
			list = objectMapper.readValue(inputStream, FunctionList.class);
		}

		if (list.exception == null) {
			List<Function> functions = list.getFunctions();
			functions.forEach(f -> {
				configureFunction(f, functionPackage);
			});
			return list.getFunctions();
		} else {
			throw new Exception(list.exception);
		}
	}

	protected abstract void configureFunction(Function f, FunctionPackage functionPackage);
}
