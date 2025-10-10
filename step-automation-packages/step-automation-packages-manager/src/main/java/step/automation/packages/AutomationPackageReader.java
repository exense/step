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

import ch.exense.commons.app.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.model.JavaAutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageDescriptorReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
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
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;
import step.plans.automation.YamlPlainTextPlan;
import step.plans.parser.yaml.YamlPlanReader;
import step.plugins.functions.types.CompositeFunctionUtils;
import step.plugins.java.GeneralScriptFunction;
import step.repositories.parser.StepsParser;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
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
public abstract class AutomationPackageReader {

    public static final String AP_VERSION_SEPARATOR = ".";
    protected static final Logger log = LoggerFactory.getLogger(AutomationPackageReader.class);
    private final PlanParser planTextPlanParser;
    protected String jsonSchemaPath;
    protected final AutomationPackageHookRegistry hookRegistry;
    private final AutomationPackageSerializationRegistry serializationRegistry;
    protected final StepClassParser stepClassParser;
    protected AutomationPackageDescriptorReader descriptorReader;

    public AutomationPackageReader(String jsonSchemaPath, AutomationPackageHookRegistry hookRegistry,
                                   AutomationPackageSerializationRegistry serializationRegistry,
                                   Configuration configuration) {
        this.jsonSchemaPath = jsonSchemaPath;
        this.hookRegistry = hookRegistry;
        this.serializationRegistry = serializationRegistry;
        this.planTextPlanParser = new PlanParser(configuration);
        this.stepClassParser = new StepClassParser(false);
    }

    abstract public AutomationPackageArchiveType getReaderForAutomationPackageType();


    /**
     * @param isLocalPackage true if the automation package is located in current classloader (i.e. all annotated keywords
     *                       can be read as {@link step.engine.plugins.LocalFunctionPlugin.LocalFunction}, but not as {@link GeneralScriptFunction}
     */
    public AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive, String apVersion, boolean isLocalPackage) throws AutomationPackageReadingException {
        return this.readAutomationPackage(automationPackageArchive, apVersion, isLocalPackage, true);
    }

    /**
     * @param isLocalPackage  true if the automation package is located in current classloader (i.e. all annotated keywords
     *                        can be read as {@link step.engine.plugins.LocalFunctionPlugin.LocalFunction}, but not as {@link GeneralScriptFunction}
     * @param scanAnnotations true if it is required to include annotated java keywords and plans as well as located in yaml descriptor
     */
    public AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive, String apVersion, boolean isLocalPackage, boolean scanAnnotations) throws AutomationPackageReadingException {
        try {
            if (automationPackageArchive.hasAutomationPackageDescriptor()) {
                try (InputStream yamlInputStream = automationPackageArchive.getDescriptorYaml()) {
                    AutomationPackageDescriptorYaml descriptorYaml = getOrCreateDescriptorReader().readAutomationPackageDescriptor(yamlInputStream, automationPackageArchive.getOriginalFileName());
                    return buildAutomationPackage(descriptorYaml, automationPackageArchive, apVersion, isLocalPackage, scanAnnotations);
                }
            } else if (automationPackageArchive.getType().equals(AutomationPackageArchiveType.JAVA) && scanAnnotations) {
                return buildAutomationPackage(null, automationPackageArchive, apVersion, isLocalPackage, scanAnnotations);
            } else {
                return null;
            }
        } catch (IOException ex) {
            throw new AutomationPackageReadingException("Unable to read the automation package", ex);
        }
    }

    protected AutomationPackageContent buildAutomationPackage(AutomationPackageDescriptorYaml descriptor, AutomationPackageArchive archive, String apVersion,
                                                              boolean isLocalPackage, boolean scanAnnotations) throws AutomationPackageReadingException {
        AutomationPackageContent res = newContentInstance();
        res.setName(resolveName(descriptor, archive, apVersion));

        if (scanAnnotations) {
            fillAutomationPackageWithAnnotatedKeywordsAndPlans(archive, isLocalPackage, res);
        }

        // apply imported fragments recursively
        if (descriptor != null) {
            fillAutomationPackageWithImportedFragments(res, descriptor, archive);
        }
        return res;
    }

    private String resolveName(AutomationPackageDescriptorYaml descriptor, AutomationPackageArchive archive, String apVersion) {
        String finalName;
        if (descriptor != null) {
            finalName = descriptor.getName();
        } else {
            finalName = Objects.requireNonNullElse(archive.getOriginalFileName(), "local-automation-package");
        }

        if (apVersion != null && !apVersion.isEmpty()) {
            finalName += AP_VERSION_SEPARATOR;
            finalName += apVersion;
        }
        return finalName;
    }

    protected AutomationPackageContent newContentInstance(){
        return new AutomationPackageContent();
    }

    abstract protected void fillAutomationPackageWithAnnotatedKeywordsAndPlans(AutomationPackageArchive archive, boolean isLocalPackage, AutomationPackageContent res) throws AutomationPackageReadingException;

    public void fillAutomationPackageWithImportedFragments(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment, AutomationPackageArchive archive) throws AutomationPackageReadingException {
        fillContentSections(targetPackage, fragment, archive);

        if (!fragment.getFragments().isEmpty()) {
            for (String importedFragmentReference : fragment.getFragments()) {
                List<URL> resources = archive.getResourcesByPattern(importedFragmentReference);
                for (URL resource : resources) {
                    try (InputStream fragmentYamlStream = resource.openStream()) {
                        fragment = getOrCreateDescriptorReader().readAutomationPackageFragment(fragmentYamlStream, importedFragmentReference, archive.getOriginalFileName());
                        fillAutomationPackageWithImportedFragments(targetPackage, fragment, archive);
                    } catch (IOException e) {
                        throw new AutomationPackageReadingException("Unable to read fragment in automation package: " + importedFragmentReference, e);
                    }
                }
            }
        }
    }

    protected void fillContentSections(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment, AutomationPackageArchive archive) throws AutomationPackageReadingException {
        targetPackage.getKeywords().addAll(fragment.getKeywords());
        targetPackage.getPlans().addAll(fragment.getPlans().stream().map(p -> getOrCreateDescriptorReader().getPlanReader().yamlPlanToPlan(p)).collect(Collectors.toList()));

        readPlainTextPlans(targetPackage, fragment, archive);

        for (Map.Entry<String, List<?>> additionalField : fragment.getAdditionalFields().entrySet()) {
            boolean hooked = hookRegistry.onAdditionalDataRead(additionalField.getKey(), additionalField.getValue(), targetPackage);
            if (!hooked) {
                log.warn("Hook not found for additional field " + additionalField.getKey() + ". The additional field has been skipped");
            }
        }
    }

    private void readPlainTextPlans(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment, AutomationPackageArchive archive) throws AutomationPackageReadingException {
        // parse plain - text plans
        for (YamlPlainTextPlan plainTextPlan : fragment.getPlansPlainText()) {
            try {
                List<URL> urls;
                boolean wildcard = false;
                if (ResourcePathMatchingResolver.containsWildcard(plainTextPlan.getFile())) {
                    wildcard = true;
                    ResourcePathMatchingResolver resourceResolver = new ResourcePathMatchingResolver(archive.getClassLoaderForMainApFile());
                    urls = resourceResolver.getResourcesByPattern(plainTextPlan.getFile());
                } else {
                    urls = List.of(archive.getResource(plainTextPlan.getFile()));
                }

                if (urls.isEmpty()) {
                    throw new AutomationPackageReadingException("No plain text plans have been found for the following path: " + plainTextPlan.getFile());
                }

                for (URL url : urls) {
                    try (InputStream is = url.openStream()) {
                        Plan parsedPlan = planTextPlanParser.parse(is, plainTextPlan.getRootType() == null ? RootArtefactType.TestCase : plainTextPlan.getRootType());
                        String planNameInYaml = plainTextPlan.getName();
                        String finalPlanName;
                        if (!wildcard) {
                            finalPlanName = (planNameInYaml == null || planNameInYaml.isEmpty()) ? plainTextPlan.getFile() : planNameInYaml;
                        } else {
                            if (planNameInYaml != null && !planNameInYaml.isEmpty()) {
                                throw new AutomationPackageReadingException("planName is not supported in combination with wildcards");
                            }
                            String urlFile = url.getFile();
                            if (urlFile != null && !urlFile.isEmpty()) {
                                int fileNameBeginIndex = urlFile.lastIndexOf(ResourcePathMatchingResolver.getPathSeparator());
                                if (fileNameBeginIndex > 0) {
                                    finalPlanName = urlFile.substring(fileNameBeginIndex + 1);
                                } else {
                                    finalPlanName = url.getPath();
                                }
                            } else {
                                finalPlanName = url.getPath();
                            }
                        }
                        YamlPlanReader.setPlanName(parsedPlan, finalPlanName);
                        parsedPlan.setCategories(plainTextPlan.getCategories());
                        targetPackage.getPlans().add(parsedPlan);
                    } catch (IOException ex) {
                        throw new AutomationPackageReadingException("Unable to read plain text plan from url: " + url.getFile(), ex);
                    }
                }

            } catch (StepsParser.ParsingException e) {
                throw new AutomationPackageReadingException("Unable to read plain text plan: " + plainTextPlan.getFile(), e);
            }
        }
    }

    public AutomationPackageContent readAutomationPackageFromJarFile(File automationPackage, String apVersion, File keywordLib) throws AutomationPackageReadingException {
        try (AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(automationPackage, keywordLib)) {
            return readAutomationPackage(automationPackageArchive, apVersion, false);
        } catch (IOException e) {
            throw new AutomationPackageReadingException("IO Exception", e);
        }
    }

    public static List<Plan> extractAnnotatedPlans(AutomationPackageArchive archive, AnnotationScanner annotationScanner, StepClassParser stepClassParser) {
        List<Plan> result = getPlans(annotationScanner, archive, stepClassParser);
        // replace null with empty collections to avoid NPEs
        result.forEach(plan -> {
            if(plan.getFunctions() == null){
                plan.setFunctions(new ArrayList<>());
            }
        });

        return result;
    }

    protected static List<Plan> getPlans(AnnotationScanner annotationScanner, AutomationPackageArchive archive, StepClassParser stepClassParser) {

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

    private static List<StepClassParserResult> getPlanFromPlansAnnotation(AnnotationScanner annotationScanner, AutomationPackageArchive archive, StepClassParser stepClassParser) {
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

    public String getDescriptorJsonSchema() {
        return getOrCreateDescriptorReader().getJsonSchema();
    }
}
