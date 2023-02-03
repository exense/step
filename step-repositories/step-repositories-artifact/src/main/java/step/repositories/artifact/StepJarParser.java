package step.repositories.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class StepJarParser {

    private static final Logger logger = LoggerFactory.getLogger(StepJarParser.class);

    private final StepClassParser stepClassParser;

    public StepJarParser() {
        stepClassParser = new StepClassParser(false);
    }

    private List<Function> getFunctions(AnnotationScanner annotationScanner, File artifact, File libraries) {
        List<Function> functions = new ArrayList<>();

        Set<Method> methods = annotationScanner.getMethodsWithAnnotation(Keyword.class);
        for (Method m : methods) {
            Keyword annotation = m.getAnnotation(Keyword.class);

            String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

            GeneralScriptFunction function = new GeneralScriptFunction();
            function.setAttributes(new HashMap<>());
            function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);
            function.setScriptFile(new DynamicValue<>(artifact.getAbsolutePath()));
            function.setLibrariesFile(new DynamicValue<>(libraries.getAbsolutePath()));
            function.setScriptLanguage(new DynamicValue<>("java"));

            functions.add(function);
        }
        return functions;
    }

    public List<Plan> getPlansForJar(File artifact, File dependency, String[] includedClasses, String[] includedAnnotations,
                                     String[] excludedClasses, String[] excludedAnnotations) {

        List<Plan> result = new ArrayList<>();

        try (AnnotationScanner annotationScanner = AnnotationScanner.forSpecificJar(artifact)) {
            // Find classes containing plans:
            Set<Class<?>> classesWithPlans = new HashSet<>();
            // Classes with @Plans annotation
            classesWithPlans.addAll(annotationScanner.getClassesWithAnnotation(Plans.class));

            // Classes with @Plan annotation in methods
            // and filter them
            Set<String> includedA = new HashSet<>(List.of(includedAnnotations));
            Set<String> excludedA = new HashSet<>(List.of(excludedAnnotations));

            for (Method m : annotationScanner.getMethodsWithAnnotation(step.junit.runners.annotations.Plan.class)) {
                logger.debug("Checking if "+m.getName()+" should be filtered...");
                boolean filtered=!includedA.isEmpty();
                for (Annotation a : m.getAnnotations()) {
                    if (excludedA.contains(a.toString())) {
                        logger.debug("Filtering out @Plan method "+m.getName()+" due to excluded annotation "+a);
                        filtered=true;
                        break;
                    } else if (includedA.contains(a.toString())) {
                        logger.debug("Including @Plan method "+m.getName()+" due to included annotation "+a);
                        filtered=false;
                    }
                }
                if (!filtered) {
                    classesWithPlans.add(m.getDeclaringClass());
                } else {
                    logger.debug(m.getName()+" has been filtered out");
                }
            }

            // Filter the classes:
            Set<String> included = new HashSet<>(List.of(includedClasses));
            Set<String> excluded = new HashSet<>(List.of(excludedClasses));
            HashSet<Class<?>> tmp = new HashSet<>();
            for (Class<?> klass : classesWithPlans) {
                logger.debug("Checking if "+klass.getName()+" should be filtered...");
                if (!excluded.contains(klass.getName()) && (included.isEmpty() || included.contains(klass.getName()))) {
                    tmp.add(klass);
                    logger.debug("Not filtering class "+klass.getName());
                } else {
                    logger.debug("Filtering out class "+klass.getName());
                }
            }
            classesWithPlans = tmp;

            // Create plans for discovered classes
            classesWithPlans.forEach(c -> result.addAll(getPlansForClass(c)));

            // Find all keywords
            List<Function> functions = getFunctions(annotationScanner, artifact, dependency);
            result.forEach(p -> p.setFunctions(functions));
        } catch (Exception e) {
            throw new RuntimeException("Exception when trying to list the plans of jar file '" + artifact.getName() + "'", e);
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
