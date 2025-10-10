package step.automation.packages;

import ch.exense.commons.app.Configuration;
import org.apache.commons.lang3.StringUtils;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.JavaAutomationPackageKeyword;
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
import step.plans.nl.parser.PlanParser;
import step.plugins.functions.types.CompositeFunctionUtils;
import step.plugins.java.GeneralScriptFunction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class JavaAutomationPackageReader extends AutomationPackageReader {

    public JavaAutomationPackageReader(String jsonSchemaPath, AutomationPackageHookRegistry hookRegistry, AutomationPackageSerializationRegistry serializationRegistry, Configuration configuration) {
        super(jsonSchemaPath, hookRegistry, serializationRegistry, configuration);
    }

    @Override
    public AutomationPackageArchiveType getReaderForAutomationPackageType() {
        return AutomationPackageArchiveType.JAVA;
    }

    @Override
    protected void fillAutomationPackageWithAnnotatedKeywordsAndPlans(AutomationPackageArchive archive, boolean isLocalPackage, AutomationPackageContent res) throws AutomationPackageReadingException {
        try (AnnotationScanner annotationScanner = archive.createAnnotationScanner()) {
            // this code duplicates the StepJarParser, but here we don't set the scriptFile and librariesFile to GeneralScriptFunctions
            // instead of this we keep the scriptFile blank and fill it further in AutomationPackageKeywordsAttributesApplier (after we upload the jar file as resource)
            List<JavaAutomationPackageKeyword> scannedKeywords = extractAnnotatedKeywords(annotationScanner, isLocalPackage, null, null);
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

    private static List<JavaAutomationPackageKeyword> extractAnnotatedKeywords(AnnotationScanner annotationScanner, boolean isLocalPackage, String scriptFile, String librariesFile) throws JsonSchemaPreparationException {
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
}
