/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.repositories.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageArchive;
import step.automation.packages.AutomationPackageKeywordsAttributesApplier;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageDescriptorReader;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.functions.Function;
import step.handlers.javahandler.Keyword;
import step.junit.runner.StepClassParser;
import step.junit.runner.StepClassParserResult;
import step.junit.runners.annotations.Plans;
import step.plans.nl.parser.PlanParser;
import step.plugins.functions.types.CompositeFunctionUtils;
import step.plugins.java.GeneralScriptFunction;
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;

public class StepJarParser {

    private static final Logger logger = LoggerFactory.getLogger(StepJarParser.class);

    private final StepClassParser stepClassParser;
    private AutomationPackageDescriptorReader automationPackageDescriptorReader;
    private final AutomationPackageKeywordsAttributesApplier automationPackagesKeywordAttributesApplier;

    public StepJarParser() {
        this(null);
    }

    public StepJarParser(ResourceManager resourceManager) {
        this.stepClassParser = new StepClassParser(false);
        this.automationPackagesKeywordAttributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);
    }

    /**
     * Lazy init for automation package descriptor reader (performance optimization)
     */
    private synchronized AutomationPackageDescriptorReader initAndGetAutomationPackageDescriptorReader() {
        if (automationPackageDescriptorReader == null) {
            automationPackageDescriptorReader = new AutomationPackageDescriptorReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH);
        }
        return automationPackageDescriptorReader;
    }

    private List<Function> getFunctions(AnnotationScanner annotationScanner, File artifact, File libraries) {
        List<Function> functions = new ArrayList<>();

        Set<Method> methods = annotationScanner.getMethodsWithAnnotation(Keyword.class);
        for (Method m : methods) {
            Keyword annotation = m.getAnnotation(Keyword.class);

            Function res;
            if (annotation.planReference() != null && !annotation.planReference().isBlank()) {
                try {
                    res = CompositeFunctionUtils.createCompositeFunction(
                            annotation, m,
                            new PlanParser().parseCompositePlanFromPlanReference(m, annotation.planReference())
                    );
                } catch (Exception ex) {
                    throw new RuntimeException("Unable to parse plan from reference", ex);
                }
            } else {
                String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

                GeneralScriptFunction function = new GeneralScriptFunction();
                function.setAttributes(new HashMap<>());
                function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);
                function.setScriptFile(new DynamicValue<>(artifact.getAbsolutePath()));
                if (libraries != null) {
                    function.setLibrariesFile(new DynamicValue<>(libraries.getAbsolutePath()));
                }
                function.getCallTimeout().setValue(annotation.timeout());
                function.setDescription(annotation.description());
                function.setScriptLanguage(new DynamicValue<>("java"));
                res = function;
            }

            functions.add(res);
        }

        try(AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(artifact)) {
            // add functions from automation package
            if(automationPackageArchive.isAutomationPackage()) {
                AutomationPackageDescriptorYaml descriptor = initAndGetAutomationPackageDescriptorReader().readAutomationPackageDescriptor(automationPackageArchive.getDescriptorYaml());
                for (AutomationPackageKeyword automationPackageKeyword : descriptor.getKeywords()) {
                    functions.add(automationPackagesKeywordAttributesApplier.applySpecialAttributesToKeyword(automationPackageKeyword, automationPackageArchive));
                }
            }
        } catch (AutomationPackageReadingException | IOException e) {
            throw new RuntimeException("Unable to process automation package", e);
        }

        return functions;
    }

    public PlansParsingResult getPlansForJar(File artifact, File dependency, String[] includedClasses, String[] includedAnnotations,
                                             String[] excludedClasses, String[] excludedAnnotations) {

        List<Plan> result = new ArrayList<>();
        List<Function> functions = new ArrayList<>();
        try (AnnotationScanner annotationScanner = AnnotationScanner.forSpecificJar(artifact)) {
            Set<String> includedA = new HashSet<>(List.of(includedAnnotations));
            Set<String> excludedA = new HashSet<>(List.of(excludedAnnotations));

            // Find classes containing plans:
            Set<Class<?>> excludedByAnnotation = new HashSet<>();
            Set<Class<?>> classesWithPlans = new HashSet<>();

            // Classes with @Plans annotation
            Set<Class<?>> classesWithPlansAnnotation = annotationScanner.getClassesWithAnnotation(Plans.class);
            for (Class<?> aClass : classesWithPlansAnnotation) {
                logger.debug("Checking if " + aClass.getName() + " should be filtered...");
                String targetName = "@Plans class " + aClass.getName();
                FilterResult filtered = isAnnotationFiltered(targetName, includedA, excludedA, aClass.getAnnotations());
                if (filtered == FilterResult.NOT_FILTERED) {
                    classesWithPlans.add(aClass);
                } else {
                    if (filtered == FilterResult.FILTERED_BY_EXCLUDED) {
                        // we have to ignore the class while scanning for '@Plan' once it is explicitly excluded
                        excludedByAnnotation.add(aClass);
                    }
                    logger.debug(aClass.getName() + " has been filtered out");
                }
            }

            // Classes with @Plan annotation in methods
            // and filter them
            for (Method m : annotationScanner.getMethodsWithAnnotation(step.junit.runners.annotations.Plan.class)) {
                if (!excludedByAnnotation.contains(m.getDeclaringClass()) && !classesWithPlans.contains(m.getDeclaringClass())) {
                    logger.debug("Checking if " + m.getName() + " should be filtered...");
                    String targetName = "@Plan method " + m.getName();
                    FilterResult filtered = isAnnotationFiltered(targetName, includedA, excludedA, m.getAnnotations());
                    if (filtered == FilterResult.NOT_FILTERED) {
                        classesWithPlans.add(m.getDeclaringClass());
                    } else {
                        logger.debug(m.getName() + " has been filtered out");
                    }
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
            classesWithPlans.forEach(c -> result.addAll(getPlansForClass(annotationScanner,c,artifact)));

            // Find all keywords
            functions = getFunctions(annotationScanner, artifact, dependency);

            // replace null with empty collections to avoid NPEs
            result.forEach(plan -> {
                if(plan.getFunctions() == null){
                    plan.setFunctions(new ArrayList<>());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Exception when trying to list the plans of jar file '" + artifact.getName() + "'", e);
        }

        return new PlansParsingResult(result, functions);
    }

    private FilterResult isAnnotationFiltered(String target, Set<String> includedA, Set<String> excludedA, Annotation[] annotations) {
        FilterResult filtered = includedA.isEmpty() ? FilterResult.NOT_FILTERED : FilterResult.FILTERED_BY_INCLUDED;
        for (Annotation a : annotations) {
            // if the annotation object is proxy, the toString() is not applicable (the format in this case is like “@step.examples.plugins.StepEETests()”)
            // so we need to check the name of annotation type to get the class name
            if (excludedA.contains(a.toString()) || excludedA.contains(a.annotationType().getName())) {
                logger.debug("Filtering out " + target + " due to excluded annotation " + a);
                filtered = FilterResult.FILTERED_BY_EXCLUDED;
                break;
            } else if (includedA.contains(a.toString()) || includedA.contains(a.annotationType().getName())) {
                logger.debug("Including " + target + " due to included annotation " + a);
                filtered = FilterResult.NOT_FILTERED;
            }
        }
        return filtered;
    }

    protected List<Plan> getPlansForClass(AnnotationScanner annotationScanner, Class<?> klass, File artifact) {

        List<Plan> result = new ArrayList<>();
        List<StepClassParserResult> plans;
        try {
            plans = stepClassParser.getPlanFromAnnotatedMethods(annotationScanner,klass);
            plans.addAll(getPlanFromPlansAnnotation(klass,artifact));

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unhandled exception when searching for plans for class '" + klass.getCanonicalName() + "'", e);
        }
        plans.forEach(p -> {
            Exception e = p.getInitializingException();
            if (e != null) {
                throw new RuntimeException("Exception when trying to create the plans for class '" + klass.getCanonicalName() + "'", e);
            }
            Plan plan = p.getPlan();
            if (plan != null) {
                result.add(plan);
            } else {
                throw new RuntimeException("No exception but also no plans for class '" + klass.getCanonicalName() + "'", e);
            }
        });
        return result;
    }

    private List<StepClassParserResult> getPlanFromPlansAnnotation(Class<?> klass, File artifact) {
        final List<StepClassParserResult> result = new ArrayList<>();

        Plans plans = klass.getAnnotation(Plans.class);
        if (plans!=null) {
            for (String file : plans.value()) {
                StepClassParserResult parserResult = null;
                try {
                    URL url = null;
                    if (file.startsWith("/")) {
                        url = new URL("jar:file:" + artifact.getAbsolutePath() + "!" + file);
                    } else {
                        url = new URL("jar:file:" + artifact.getAbsolutePath() + "!/" +
                                klass.getPackageName().replace(".", "/") + "/" + file);
                    }

                    JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                    jarURLConnection.setUseCaches(false);
                    try ( InputStream stream = jarURLConnection.getInputStream()) {

                        if (stream != null) {
                            // create plan from plain-text or from yaml
                            parserResult = stepClassParser.createPlan(klass, file, stream);
                        } else {
                            throw new FileNotFoundException(file);
                        }
                        if (parserResult.getPlan() != null) {
                            StepClassParser.setPlanName(parserResult.getPlan(), file);
                        }
                    }
                } catch (Exception e) {
                    parserResult = new StepClassParserResult(file, null,  e);
                }
                result.add(parserResult);
            }
        }
        return result;
    }

    public static class PlansParsingResult {

        private final List<Plan> plans;
        private final List<Function> functions;

        public PlansParsingResult(List<Plan> plans, List<Function> functions) {
            this.plans = plans;
            this.functions = functions;
        }

        public List<Plan> getPlans() {
            return plans;
        }

        public List<Function> getFunctions() {
            return functions;
        }
    }

    private enum FilterResult {
        NOT_FILTERED,
        FILTERED_BY_INCLUDED,
        FILTERED_BY_EXCLUDED
    }
}
