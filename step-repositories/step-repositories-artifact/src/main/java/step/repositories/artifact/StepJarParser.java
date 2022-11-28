package step.repositories.artifact;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.functions.Function;
import step.handlers.javahandler.Keyword;
import step.junit.runner.StepClassParser;
import step.junit.runner.StepClassParserResult;
import step.junit.runners.annotations.Plans;
import step.plugins.java.GeneralScriptFunction;

public class StepJarParser {

	private final StepClassParser stepClassParser;

	public StepJarParser() {
		stepClassParser = new StepClassParser(false);
	}

	private List<Function> getFunctions(AnnotationScanner annotationScanner, File file) {
		List<Function> functions = new ArrayList<>();

		Set<Method> methods = annotationScanner.getMethodsWithAnnotation(Keyword.class);
		for (Method m : methods) {
			Keyword annotation = m.getAnnotation(Keyword.class);

			String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

			GeneralScriptFunction function = new GeneralScriptFunction();
			function.setAttributes(new HashMap<>());
			function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);
			function.setScriptFile(new DynamicValue<>(file.getAbsolutePath()));
			function.setScriptLanguage(new DynamicValue<>("java"));

			functions.add(function);
		}
		return functions;
	}

	public List<Plan> getPlansForJar(File jarFile) {

		List<Plan> result = new ArrayList<>();

		try (AnnotationScanner annotationScanner = AnnotationScanner.forSpecificJar(jarFile)) {
			// Find classes containing plans
			// Classes with @Plans annotation
			Set<Class<?>> classesWithPlans = new HashSet<>();
			classesWithPlans.addAll(new HashSet<>(annotationScanner.getClassesWithAnnotation(Plans.class)));
			// Classes with @Plan annotation in methods
			classesWithPlans.addAll(annotationScanner.getMethodsWithAnnotation(step.junit.runners.annotations.Plan.class).stream()
					.map(Method::getDeclaringClass).collect(Collectors.toSet()));
			// Create plans for discovered classes
			classesWithPlans.forEach(c -> result.addAll(getPlansForClass(c)));
			
			// Find all keywords
			List<Function> functions = getFunctions(annotationScanner, jarFile);
			result.forEach(p -> p.setFunctions(functions));
		} catch (Exception e) {
			throw new RuntimeException(
					"Exception when trying to list the plans of jar file '" + jarFile.getName() + "'", e);
		}

		return result;
	}

	protected List<Plan> getPlansForClass(Class<?> klass) {

		List<Plan> result = new ArrayList<>();
		List<StepClassParserResult> plans;
		try {
			plans = stepClassParser.createPlansForClass(klass);
			plans.forEach(p -> {
				Plan plan = p.getPlan();
				if (plan != null) {
					result.add(plan);
				} else {
					// TODO handle error
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(
					"Exception when trying to create the plans for class '" + klass.getCanonicalName() + "'", e);
		}
		return result;
	}
}
