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
package step.automation.packages;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.hooks.AutomationPackageHookRegistry;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.JavaAutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageDescriptorReader;
import step.core.automation.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.engine.plugins.LocalFunctionPlugin;
import step.functions.Function;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator;
import step.junit.runner.StepClassParser;
import step.junit.runner.StepClassParserResult;
import step.junit.runners.annotations.Plans;
import step.plans.nl.parser.PlanParser;
import step.plugins.functions.types.CompositeFunctionUtils;
import step.plugins.java.GeneralScriptFunction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Designed to read the automation package content from some source (for instance, from jar archive).
 * It is important that the {@link AutomationPackageReader} doesn't affect the global context, i.e. it doesn't persist any plan, keyword or resource.
 * Instead of this, it prepares the {@link AutomationPackageContent} with {@link JavaAutomationPackageKeyword}
 * containing the draft instances of {@link Function}, without any references to uploaded resources (because
 * these resources are not stored yet).
 */
public class AutomationPackageReader {

    protected static final Logger log = LoggerFactory.getLogger(AutomationPackageReader.class);

    protected String jsonSchemaPath;
    protected final AutomationPackageHookRegistry hookRegistry;
    private final AutomationPackageSerializationRegistry serializationRegistry;
    protected final StepClassParser stepClassParser;
    protected AutomationPackageDescriptorReader descriptorReader;

    public AutomationPackageReader(String jsonSchemaPath, AutomationPackageHookRegistry hookRegistry, AutomationPackageSerializationRegistry serializationRegistry) {
        this.jsonSchemaPath = jsonSchemaPath;
        this.hookRegistry = hookRegistry;
        this.serializationRegistry = serializationRegistry;
        this.stepClassParser = new StepClassParser(false);
    }

    /**
     * @param isLocalPackage true if the automation package is located in current classloader (i.e. all annotated keywords
     *                       can be read as {@link step.engine.plugins.LocalFunctionPlugin.LocalFunction}, but not as {@link GeneralScriptFunction}
     */
    public AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive, boolean isLocalPackage) throws AutomationPackageReadingException {
        return this.readAutomationPackage(automationPackageArchive, isLocalPackage, true);
    }

    /**
     * @param isLocalPackage  true if the automation package is located in current classloader (i.e. all annotated keywords
     *                        can be read as {@link step.engine.plugins.LocalFunctionPlugin.LocalFunction}, but not as {@link GeneralScriptFunction}
     * @param scanAnnotations true if it is required to include annotated java keywords and plans as well as located in yaml descriptor
     */
    public AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive, boolean isLocalPackage, boolean scanAnnotations) throws AutomationPackageReadingException {
        try {
            if (automationPackageArchive.hasAutomationPackageDescriptor()) {
                try (InputStream yamlInputStream = automationPackageArchive.getDescriptorYaml()) {
                    AutomationPackageDescriptorYaml descriptorYaml = getOrCreateDescriptorReader().readAutomationPackageDescriptor(yamlInputStream, automationPackageArchive.getOriginalFileName());
                    return buildAutomationPackage(descriptorYaml, automationPackageArchive, isLocalPackage, scanAnnotations);
                }
            } else if (automationPackageArchive.getType().equals(AutomationPackageArchiveType.JAVA) && scanAnnotations) {
                return buildAutomationPackage(null, automationPackageArchive, isLocalPackage, scanAnnotations);
            } else {
                return null;
            }
        } catch (IOException ex) {
            throw new AutomationPackageReadingException("Unable to read the automation package", ex);
        }
    }

    protected AutomationPackageContent buildAutomationPackage(AutomationPackageDescriptorYaml descriptor, AutomationPackageArchive archive, boolean isLocalPackage, boolean scanAnnotations) throws AutomationPackageReadingException {
        AutomationPackageContent res = newContentInstance();
        res.setName(resolveName(descriptor, archive));

        if (scanAnnotations) {
            fillAutomationPackageWithAnnotatedKeywordsAndPlans(archive, isLocalPackage, res);
        }

        // apply imported fragments recursively
        if (descriptor != null) {
            fillAutomationPackageWithImportedFragments(res, descriptor, archive);
        }
        return res;
    }

    private String resolveName(AutomationPackageDescriptorYaml descriptor, AutomationPackageArchive archive) {
        if (descriptor != null) {
            return descriptor.getName();
        } else {
            return Objects.requireNonNullElse(archive.getOriginalFileName(), "local-automation-package");
        }
    }

    protected AutomationPackageContent newContentInstance(){
        return new AutomationPackageContent();
    }

    private void fillAutomationPackageWithAnnotatedKeywordsAndPlans(AutomationPackageArchive archive, boolean isLocalPackage, AutomationPackageContent res) throws AutomationPackageReadingException {

        // for file-based packages we create class loader for file, otherwise we just use class loader from archive
        File originalFile = archive.getOriginalFile();
        try (AnnotationScanner annotationScanner = originalFile != null ? AnnotationScanner.forSpecificJar(originalFile) : AnnotationScanner.forAllClassesFromClassLoader(archive.getClassLoader())) {

            // this code duplicates the StepJarParser, but here we don't set the scriptFile and librariesFile to GeneralScriptFunctions
            // instead of this we keep the scriptFile blank and fill it further in AutomationPackageKeywordsAttributesApplier (after we upload the jar file as resource)
            List<JavaAutomationPackageKeyword> scannedKeywords = extractAnnotatedKeywords(annotationScanner, isLocalPackage, null, null);
            if(!scannedKeywords.isEmpty()){
                log.info("{} annotated keywords found in automation package {}", scannedKeywords.size(), StringUtils.defaultString(archive.getOriginalFileName()));
            }
            res.getKeywords().addAll(scannedKeywords);

            List<Plan> annotatedPlans = extractAnnotatedPlans(archive, annotationScanner, null, null, null, null, stepClassParser);
            if (!annotatedPlans.isEmpty()) {
                log.info("{} annotated plans found in automation package {}", annotatedPlans.size(), StringUtils.defaultString(archive.getOriginalFileName()));
            }
            res.getPlans().addAll(annotatedPlans);
        } catch (JsonSchemaPreparationException e) {
            throw new AutomationPackageReadingException("Cannot read the json schema from annotated keyword", e);
        }
    }

    public static List<JavaAutomationPackageKeyword> extractAnnotatedKeywords(AnnotationScanner annotationScanner, boolean isLocalPackage, String scriptFile, String librariesFile) throws JsonSchemaPreparationException {
        List<JavaAutomationPackageKeyword> scannedKeywords = new ArrayList<>();
        Set<Method> methods = annotationScanner.getMethodsWithAnnotation(Keyword.class);
        if(!methods.isEmpty()) {
            KeywordJsonSchemaCreator annotatedKeywordJsonSchemaCreator = new KeywordJsonSchemaCreator();
            for (Method m : methods) {
                Keyword annotation = m.getAnnotation(Keyword.class);
                if (annotation == null) {
                    log.warn("Keyword annotation is not found for method " + m.getName());
                    continue;
                }

                Function f;
                if (isCompositeFunction(annotation)) {
                    f = createCompositeFunction(m, annotation);
                } else {
                    if (!isLocalPackage) {
                        String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

                        GeneralScriptFunction function = new GeneralScriptFunction();
                        function.setAttributes(new HashMap<>());
                        function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);

                        // to be filled by AutomationPackageKeywordsAttributesApplier
                        if (scriptFile != null) {
                            function.setScriptFile(new DynamicValue<>(scriptFile));
                        }

                        // libraries file is not used is automation package (only required for compatibility with StepJarParser)
                        if (librariesFile != null) {
                            function.setLibrariesFile(new DynamicValue<>(librariesFile));
                        }

                        function.getCallTimeout().setValue(annotation.timeout());
                        function.setScriptLanguage(new DynamicValue<>("java"));
                        f = function;
                    } else {
                        f = LocalFunctionPlugin.createLocalFunction(m, annotation);
                    }
                }

                f.setDescription(annotation.description());
                f.setSchema(annotatedKeywordJsonSchemaCreator.createJsonSchemaForKeyword(m));

                String htmlTemplate = f.getAttributes().remove("htmlTemplate");
                if (htmlTemplate != null && !htmlTemplate.isEmpty()) {
                    f.setHtmlTemplate(htmlTemplate);
                    f.setUseCustomTemplate(true);
                }

                scannedKeywords.add(new JavaAutomationPackageKeyword(f));
            }
        }
        return scannedKeywords;
    }

    private static Function createCompositeFunction(Method m, Keyword annotation) {
        Function f;
        try {
            f = CompositeFunctionUtils.createCompositeFunction(
                    annotation, m,
                    new PlanParser().parseCompositePlanFromPlanReference(m, annotation.planReference())
            );
        } catch (Exception ex) {
            throw new RuntimeException("Unable to parse plan from reference", ex);
        }
        return f;
    }

    private static boolean isCompositeFunction(Keyword annotation) {
        return annotation.planReference() != null && !annotation.planReference().isBlank();
    }

    public void fillAutomationPackageWithImportedFragments(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment, AutomationPackageArchive archive) throws AutomationPackageReadingException {
        fillContentSections(targetPackage, fragment);

        if (!fragment.getFragments().isEmpty()) {
            for (String importedFragmentReference : fragment.getFragments()) {
                try (InputStream fragmentYamlStream = archive.getResourceAsStream(importedFragmentReference)) {
                    fragment = getOrCreateDescriptorReader().readAutomationPackageFragment(fragmentYamlStream, importedFragmentReference, archive.getOriginalFileName());
                    fillAutomationPackageWithImportedFragments(targetPackage, fragment, archive);
                } catch (IOException e) {
                    throw new AutomationPackageReadingException("Unable to read fragment in automation package: " + importedFragmentReference, e);
                }
            }
        }
    }

    protected void fillContentSections(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment) {
        targetPackage.getKeywords().addAll(fragment.getKeywords());
        targetPackage.getPlans().addAll(fragment.getPlans().stream().map(p -> getOrCreateDescriptorReader().getPlanReader().yamlPlanToPlan(p)).collect(Collectors.toList()));
        for (Map.Entry<String, List<?>> additionalField : fragment.getAdditionalFields().entrySet()) {
            boolean hooked = hookRegistry.onAdditionalDataRead(additionalField.getKey(), additionalField.getValue(), targetPackage);
            if (!hooked) {
                log.warn("Hook not found for additional field " + additionalField.getKey() + ". The additional field has been skipped");
            }
        }
    }

    public AutomationPackageContent readAutomationPackageFromJarFile(File automationPackageJar) throws AutomationPackageReadingException {
        try (AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(automationPackageJar, automationPackageJar.getName())) {
            return readAutomationPackage(automationPackageArchive, false);
        } catch (IOException e) {
            throw new AutomationPackageReadingException("IO Exception", e);
        }
    }

    public static List<Plan> extractAnnotatedPlans(AutomationPackageArchive archive, AnnotationScanner annotationScanner, String[] includedClasses, String[] includedAnnotations, String[] excludedClasses, String[] excludedAnnotations, StepClassParser stepClassParser) {
        List<Plan> result = new ArrayList<>();
        Set<String> includedA = new HashSet<>(includedAnnotations == null ? new ArrayList<>() : List.of(includedAnnotations));
        Set<String> excludedA = new HashSet<>(excludedAnnotations == null ? new ArrayList<>() : List.of(excludedAnnotations));

        // Find classes containing plans:
        Set<Class<?>> excludedByAnnotation = new HashSet<>();
        Set<Class<?>> classesWithPlans = new HashSet<>();

        // Classes with @Plans annotation
        Set<Class<?>> classesWithPlansAnnotation = annotationScanner.getClassesWithAnnotation(Plans.class);
        for (Class<?> aClass : classesWithPlansAnnotation) {
            log.debug("Checking if " + aClass.getName() + " should be filtered...");
            String targetName = "@Plans class " + aClass.getName();
            FilterResult filtered = isAnnotationFiltered(targetName, includedA, excludedA, aClass.getAnnotations());
            if (filtered == FilterResult.NOT_FILTERED) {
                classesWithPlans.add(aClass);
            } else {
                if (filtered == FilterResult.FILTERED_BY_EXCLUDED) {
                    // we have to ignore the class while scanning for '@Plan' once it is explicitly excluded
                    excludedByAnnotation.add(aClass);
                }
                log.debug(aClass.getName() + " has been filtered out");
            }
        }

        Set<Method> excludedMethods = new HashSet<>();

        // Classes with @Plan annotation in methods
        // and filter them
        for (Method m : annotationScanner.getMethodsWithAnnotation(step.junit.runners.annotations.Plan.class)) {
            if (!excludedByAnnotation.contains(m.getDeclaringClass())) {
                log.debug("Checking if " + m.getName() + " should be filtered...");
                String targetName = "@Plan method " + m.getName();
                FilterResult filtered = isAnnotationFiltered(targetName, includedA, excludedA, m.getAnnotations());
                if (filtered == FilterResult.NOT_FILTERED) {
                    classesWithPlans.add(m.getDeclaringClass());
                } else {
                    excludedMethods.add(m);
                    log.debug(m.getName() + " has been filtered out");
                }
            }
        }

        // Filter the classes:
        Set<String> included = new HashSet<>(includedClasses == null ? new ArrayList<>() : List.of(includedClasses));
        Set<String> excluded = new HashSet<>(excludedClasses == null ? new ArrayList<>() : List.of(excludedClasses));
        HashSet<Class<?>> tmp = new HashSet<>();
        for (Class<?> klass : classesWithPlans) {
            log.debug("Checking if "+klass.getName()+" should be filtered...");
            if (!excluded.contains(klass.getName()) && (included.isEmpty() || included.contains(klass.getName()))) {
                tmp.add(klass);
                log.debug("Not filtering class "+klass.getName());
            } else {
                log.debug("Filtering out class "+klass.getName());
            }
        }
        classesWithPlans = tmp;

        // Create plans for discovered classes
        classesWithPlans.forEach(c -> result.addAll(getPlansForClass(annotationScanner, c, archive, stepClassParser, excludedMethods)));

        // replace null with empty collections to avoid NPEs
        result.forEach(plan -> {
            if(plan.getFunctions() == null){
                plan.setFunctions(new ArrayList<>());
            }
        });

        return result;
    }

    private static FilterResult isAnnotationFiltered(String target, Set<String> includedA, Set<String> excludedA, Annotation[] annotations) {
        FilterResult filtered = includedA.isEmpty() ? FilterResult.NOT_FILTERED : FilterResult.FILTERED_BY_INCLUDED;
        for (Annotation a : annotations) {
            // if the annotation object is proxy, the toString() is not applicable (the format in this case is like “@step.examples.plugins.StepEETests()”)
            // so we need to check the name of annotation type to get the class name
            if (excludedA.contains(a.toString()) || excludedA.contains(a.annotationType().getName())) {
                log.debug("Filtering out " + target + " due to excluded annotation " + a);
                filtered = FilterResult.FILTERED_BY_EXCLUDED;
                break;
            } else if (includedA.contains(a.toString()) || includedA.contains(a.annotationType().getName())) {
                log.debug("Including " + target + " due to included annotation " + a);
                filtered = FilterResult.NOT_FILTERED;
            }
        }
        return filtered;
    }

    protected static List<Plan> getPlansForClass(AnnotationScanner annotationScanner, Class<?> klass, AutomationPackageArchive archive, StepClassParser stepClassParser, Set<Method> excludedMethods) {

        List<Plan> result = new ArrayList<>();
        List<StepClassParserResult> plans;
        try {
            plans = stepClassParser.getPlanFromAnnotatedMethods(annotationScanner, klass, excludedMethods);
            plans.addAll(getPlanFromPlansAnnotation(klass, archive, stepClassParser));
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

    private static List<StepClassParserResult> getPlanFromPlansAnnotation(Class<?> klass, AutomationPackageArchive archive, StepClassParser stepClassParser) {
        final List<StepClassParserResult> result = new ArrayList<>();

        Plans plans = klass.getAnnotation(Plans.class);
        if (plans != null) {
            for (String file : plans.value()) {
                StepClassParserResult parserResult = null;
                try {
                    ClassLoader classLoader = null;
                    File originalFile = archive.getOriginalFile();
                    if (originalFile != null) {
                        classLoader = new URLClassLoader(new URL[]{originalFile.toURI().toURL()});
                    } else {
                        classLoader = archive.getClassLoader();
                    }
                    try (InputStream stream = classLoader.getResourceAsStream(file)) {
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
                    parserResult = new StepClassParserResult(file, null, e);
                }
                result.add(parserResult);
            }
        }
        return result;
    }

    protected synchronized AutomationPackageDescriptorReader getOrCreateDescriptorReader() {
        // lazy initialization of descriptor reader (performance issue)
        if (descriptorReader == null) {
            this.descriptorReader = new AutomationPackageDescriptorReader(jsonSchemaPath, serializationRegistry);
        }
        return descriptorReader;
    }

    public synchronized void updateJsonSchema(String jsonSchemaPath){
        log.info("Change json schema for automation package to {}", jsonSchemaPath);
        this.jsonSchemaPath = jsonSchemaPath;
        this.descriptorReader = null;
    }

    private enum FilterResult {
        NOT_FILTERED,
        FILTERED_BY_INCLUDED,
        FILTERED_BY_EXCLUDED
    }
}
