package step.plugins.functions.types;

import step.core.accessors.AbstractOrganizableObject;
import step.handlers.javahandler.Keyword;

import java.lang.reflect.Method;
import java.util.HashMap;

public class CompositeFunctionUtils {
	public static CompositeFunction createCompositeFunction(Keyword annotation, Method m, String planId) {
		CompositeFunction function = new CompositeFunction();

		String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

		function.getCallTimeout().setValue(annotation.timeout());
		function.setAttributes(new HashMap<>());
		function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);
		try {
			function.setPlanId(planId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return function;
	}

}
