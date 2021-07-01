package step.functions.packages;

import java.util.List;

import step.functions.Function;

public interface FunctionPackageHandler {

	boolean isValidForPackage(FunctionPackage functionPackage);

	List<Function> buildFunctions(FunctionPackage functionPackage, boolean preview) throws Exception;

}