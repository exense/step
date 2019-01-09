package step.functions.base.types;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import step.functions.base.types.handler.LocalFunctionHandler;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.handlers.javahandler.Keyword;

public class LocalFunctionType extends AbstractFunctionType<LocalFunction> {

	public static final String LOCALFUNCTIONCLASSES_PREFIX = "step.functions.base.defaults";

	@Override
	public void init() {
		super.init();
	}

	@Override
	public String getHandlerChain(LocalFunction function) {
		return LocalFunctionHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(LocalFunction function) {
		Map<String, String> props = new HashMap<>();
		return props;
	}

	@Override
	public void setupFunction(LocalFunction function) throws SetupFunctionException {

	}

	@Override
	public LocalFunction copyFunction(LocalFunction function) throws FunctionTypeException {
		LocalFunction copy = super.copyFunction(function);
		return copy;
	}

	@Override
	public LocalFunction newFunction() {
		return new LocalFunction();
	}

	public static List<String> getLocalKeywordList() throws Exception {

		List<String> keywordList = new ArrayList<>();

		try {
			Set<Method> methods = new Reflections(LOCALFUNCTIONCLASSES_PREFIX, new MethodAnnotationsScanner()).getMethodsAnnotatedWith(Keyword.class);

			for(Method method:methods) {
				keywordList.add(method.getName());
			}

			return keywordList;
		} catch (Exception e) {
			String errorMsg = "Error while looking for methods annotated with @Keyword in base classloader";
			throw new Exception(errorMsg, e);
		}
	}

	public static Set<Class<?>> getLocalKeywordClasses() throws Exception {
		Set<Class<?>> classSet = new HashSet<>();

		for(Method m : new Reflections(LOCALFUNCTIONCLASSES_PREFIX, new MethodAnnotationsScanner()).getMethodsAnnotatedWith(Keyword.class)){
			classSet.add(m.getDeclaringClass());
		}
		return classSet;
	}

	public static List<String> getLocalKeywordClassNames() throws Exception {
		List<String> classNames = new ArrayList<>();

		for(Class clazz : getLocalKeywordClasses()){
			classNames.add(clazz.getName());
		}
		return classNames;
	}

}
