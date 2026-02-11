package step.automation.packages;

import ch.exense.commons.app.Configuration;
import org.apache.commons.lang3.StringUtils;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.ScriptAutomationPackageKeyword;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.engine.plugins.LocalFunctionPlugin;
import step.functions.Function;
import step.functions.manager.FunctionManagerImpl;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator;
import step.junit.runner.StepClassParser;
import step.junit.runner.StepClassParserResult;
import step.junit.runners.annotations.Plans;
import step.plans.nl.parser.PlanParser;
import step.plans.parser.yaml.YamlPlanReader;
import step.plugins.functions.types.CompositeFunctionUtils;
import step.plugins.java.GeneralScriptFunction;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class JavaAutomationPackageReader extends AutomationPackageReader<JavaAutomationPackageArchive> {

    protected final StepClassParser stepClassParser;

    public JavaAutomationPackageReader(String jsonSchemaPath, AutomationPackageHookRegistry hookRegistry, AutomationPackageSerializationRegistry serializationRegistry, Configuration configuration) {
        super(jsonSchemaPath, hookRegistry, serializationRegistry, configuration, JavaAutomationPackageArchive.class);
        this.stepClassParser = new StepClassParser(false);
    }

    @Override
    public boolean isValidForFile(File file) {
        return JavaAutomationPackageArchive.isValidForFile(file);
    }

    @Override
    public String getAutomationPackageType() {
        return JavaAutomationPackageArchive.TYPE;
    }

    @Override
    public List<String> getSupportedFileTypes() {
        return List.of("ZIP archive","JAR file","Directory");
    }

    @Override
    protected void fillAutomationPackageWithAnnotatedKeywordsAndPlans(JavaAutomationPackageArchive archive, boolean isClasspathBased, AutomationPackageContent res) throws AutomationPackageReadingException {
        try (AnnotationScanner annotationScanner = archive.createAnnotationScanner()) {
            // this code duplicates the StepJarParser, but here we don't set the scriptFile and librariesFile to GeneralScriptFunctions
            // instead of this we keep the scriptFile blank and fill it further in AutomationPackageKeywordsAttributesApplier (after we upload the jar file as resource)
            List<ScriptAutomationPackageKeyword> scannedKeywords = extractAnnotatedKeywords(annotationScanner, isClasspathBased, null, null);
            if (!scannedKeywords.isEmpty()) {
                log.info("{} annotated keywords found in automation package {}", scannedKeywords.size(), StringUtils.defaultString(archive.getOriginalFileName()));
            }
            res.getKeywords().addAll(scannedKeywords);

            List<Plan> annotatedPlans = extractAnnotatedPlans(archive, annotationScanner, stepClassParser);
            if (!annotatedPlans.isEmpty()) {
                log.info("{} annotated plans found in automation package {}", annotatedPlans.size(), StringUtils.defaultString(archive.getOriginalFileName()));
            }
            res.getPlans().addAll(annotatedPlans);
        } catch (JsonSchemaPreparationException e) {
            throw new AutomationPackageReadingException("Cannot read the json schema from annotated keyword", e);
        } catch (Throwable e) {
            throw new AutomationPackageReadingException("Unexpected error while extracting annotated keyword: " + e, e);
        }
    }

    private static List<Plan> extractAnnotatedPlans(JavaAutomationPackageArchive archive, AnnotationScanner annotationScanner, StepClassParser stepClassParser) {
        List<Plan> result = getPlans(annotationScanner, archive, stepClassParser);
        // replace null with empty collections to avoid NPEs
        result.forEach(plan -> {
            if(plan.getFunctions() == null){
                plan.setFunctions(new ArrayList<>());
            }
        });

        return result;
    }

    private static List<Plan> getPlans(AnnotationScanner annotationScanner, JavaAutomationPackageArchive archive, StepClassParser stepClassParser) {

        List<Plan> result = new ArrayList<>();
        List<StepClassParserResult> plans;
        try {
            plans = stepClassParser.getPlanFromAnnotatedMethods(annotationScanner);
            plans.addAll(getPlanFromPlansAnnotation(annotationScanner, archive, stepClassParser));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unhandled exception when searching for plans", e);
        }
        plans.forEach(p -> {
            Exception e = p.getInitializingException();
            if (e != null) {
                throw new RuntimeException("Exception when trying to create the plans", e);
            }
            Plan plan = p.getPlan();
            if (plan != null) {
                result.add(plan);
            } else {
                throw new RuntimeException("No exception but also no plans", e);
            }
        });
        return result;
    }

    private static List<StepClassParserResult> getPlanFromPlansAnnotation(AnnotationScanner annotationScanner, JavaAutomationPackageArchive archive, StepClassParser stepClassParser) {
        final List<StepClassParserResult> result = new ArrayList<>();
        for (Class<?> klass : annotationScanner.getClassesWithAnnotation(Plans.class)) {
            Plans plans = klass.getAnnotation(Plans.class);
            if (plans != null) {
                for (String file : plans.value()) {
                    StepClassParserResult parserResult = null;
                    ClassLoader classLoader = archive.getClassLoaderForMainApFile();
                    boolean createdClassloader = false;
                    try {
                        File originalFile = archive.getOriginalFile();
                        if (classLoader == null) {
                            //Fall back to creating a new class loader from original file
                            if (originalFile != null) {
                                createdClassloader = true;
                                classLoader = new URLClassLoader(new URL[]{originalFile.toURI().toURL()});
                            } else {
                                throw new RuntimeException("Neither the archive classloader or archive file are set");
                            }
                        }
                        try (InputStream stream = classLoader.getResourceAsStream(file)) {
                            if (stream != null) {
                                // create plan from plain-text or from yaml
                                parserResult = stepClassParser.createPlan(klass, file, stream);
                            } else {
                                throw new FileNotFoundException(file);
                            }
                            if (parserResult.getPlan() != null) {
                                YamlPlanReader.setPlanName(parserResult.getPlan(), file);
                            }
                        }
                    } catch (Exception e) {
                        parserResult = new StepClassParserResult(file, null, e);
                    } finally {
                        if (createdClassloader && classLoader instanceof AutoCloseable) {
                            try {
                                ((AutoCloseable) classLoader).close();
                            } catch (Exception e) {
                                log.error("Unable to close the classloader created from provided package file '{}' after reading its content.", archive.getOriginalFile().getName());
                            }
                        }
                    }
                    result.add(parserResult);
                }
            }
        }
        return result;
    }

    private static List<ScriptAutomationPackageKeyword> extractAnnotatedKeywords(AnnotationScanner annotationScanner, boolean isClasspathBased, String scriptFile, String librariesFile) throws JsonSchemaPreparationException {
        List<ScriptAutomationPackageKeyword> scannedKeywords = new ArrayList<>();
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
                    if (!isClasspathBased) {
                        String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

                        GeneralScriptFunction function = new GeneralScriptFunction();
                        function.setAttributes(new HashMap<>());
                        function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);

                        // to be filled by AutomationPackageKeywordsAttributesApplier
                        if (scriptFile != null) {
                            function.setScriptFile(new DynamicValue<>(scriptFile));
                        }

                        if (librariesFile != null) {
                            function.setLibrariesFile(new DynamicValue<>(librariesFile));
                        }

                        function.getCallTimeout().setValue(annotation.timeout());
                        FunctionManagerImpl.applyRoutingFromAnnotation(function, annotation);

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

                scannedKeywords.add(new ScriptAutomationPackageKeyword(f));
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

    /** Convenient method for test
     * @param automationPackage the JAR file to be read
     * @param apVersion the automation package version
     * @param keywordLib the package library file
     * @return the automation package content raed from the provided files
     * @throws AutomationPackageReadingException in case of error
     */
    public AutomationPackageContent readAutomationPackageFromJarFile(File automationPackage, String apVersion, File keywordLib) throws AutomationPackageReadingException {
        try (JavaAutomationPackageArchive automationPackageArchive = new JavaAutomationPackageArchive(automationPackage, keywordLib, null)) {
            return readAutomationPackage(automationPackageArchive, apVersion, false);
        } catch (IOException e) {
            throw new AutomationPackageReadingException("IO Exception", e);
        }
    }
}
