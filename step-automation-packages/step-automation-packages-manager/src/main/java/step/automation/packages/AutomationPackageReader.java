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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.model.ScriptAutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageDescriptorReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.plans.Plan;
import step.functions.Function;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;
import step.plans.automation.YamlPlainTextPlan;
import step.plans.parser.yaml.YamlPlanReader;
import step.plugins.java.GeneralScriptFunction;
import step.repositories.parser.StepsParser;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Designed to read the automation package content from some source (for instance, from jar archive).
 * It is important that the {@link AutomationPackageReader} doesn't affect the global context, i.e. it doesn't persist any plan, keyword or resource.
 * Instead of this, it prepares the {@link AutomationPackageContent} with for instance {@link ScriptAutomationPackageKeyword}
 * containing the draft instances of {@link Function}, without any references to uploaded resources (because
 * these resources are not stored yet).
 */
public abstract class AutomationPackageReader<T extends AutomationPackageArchive> {

    public static final String AP_VERSION_SEPARATOR = ".";
    protected static final Logger log = LoggerFactory.getLogger(AutomationPackageReader.class);
    private final PlanParser planTextPlanParser;
    protected String jsonSchemaPath;
    protected final AutomationPackageHookRegistry hookRegistry;
    private final AutomationPackageSerializationRegistry serializationRegistry;
    protected AutomationPackageDescriptorReader descriptorReader;
    protected final Class<? extends AutomationPackageArchive> automationPackageArchiveClass;

    public AutomationPackageReader(String jsonSchemaPath, AutomationPackageHookRegistry hookRegistry,
                                   AutomationPackageSerializationRegistry serializationRegistry,
                                   Configuration configuration, Class<? extends AutomationPackageArchive> automationPackageArchiveClass) {
        this.jsonSchemaPath = jsonSchemaPath;
        this.hookRegistry = hookRegistry;
        this.serializationRegistry = serializationRegistry;
        this.planTextPlanParser = new PlanParser(configuration);
        this.automationPackageArchiveClass = automationPackageArchiveClass;
    }

    public AutomationPackageArchive createAutomationPackageArchive(File automationPackageFile, File keywordLibFile, String defaultName) {
        try {
            Constructor<? extends AutomationPackageArchive> declaredConstructor = automationPackageArchiveClass.getDeclaredConstructor(File.class, File.class, String.class);
            return declaredConstructor.newInstance(automationPackageFile, keywordLibFile, defaultName);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("The constructor for type " + getAutomationPackageType() + " is invalid", e);
        }
    }

    abstract public boolean isValidForFile(File file);

    abstract public String getAutomationPackageType();

    abstract public List<String> getSupportedFileTypes();

    /**
     * @param isLocalPackage true if the automation package is located in current classloader (i.e. all annotated keywords
     *                       can be read as {@link step.engine.plugins.LocalFunctionPlugin.LocalFunction}, but not as {@link GeneralScriptFunction}
     */
    public AutomationPackageContent readAutomationPackage(T automationPackageArchive, String apVersion, boolean isLocalPackage) throws AutomationPackageReadingException {
        return this.readAutomationPackage(automationPackageArchive, apVersion, isLocalPackage, true);
    }

    /**
     * @param isLocalPackage  true if the automation package is located in current classloader (i.e. all annotated keywords
     *                        can be read as {@link step.engine.plugins.LocalFunctionPlugin.LocalFunction}, but not as {@link GeneralScriptFunction}
     * @param scanAnnotations true if it is required to include annotated java keywords and plans as well as located in yaml descriptor
     */
    public AutomationPackageContent readAutomationPackage(T archive, String apVersion, boolean isLocalPackage, boolean scanAnnotations) throws AutomationPackageReadingException {
        if (archive.hasAutomationPackageDescriptor()) {
            AutomationPackageDescriptorYaml descriptorYaml = getOrCreateDescriptorReader().readAutomationPackageDescriptor(archive.getDescriptorYamlUrl(), archive.getOriginalFileName());
            return buildAutomationPackage(descriptorYaml, archive, apVersion, isLocalPackage, scanAnnotations);
        } else if (scanAnnotations) {
            return buildAutomationPackage(null, archive, apVersion, isLocalPackage, scanAnnotations);
        } else {
            return null;
        }
    }


    protected AutomationPackageContent buildAutomationPackage(AutomationPackageDescriptorYaml descriptor, T archive, String apVersion,
                                                              boolean isLocalPackage, boolean scanAnnotations) throws AutomationPackageReadingException {
        AutomationPackageContent res = newContentInstance();
        String baseName = resolveName(descriptor, archive);
        res.setBaseName(baseName);
        res.setName(resolveUniqueName(baseName, apVersion));

        if (scanAnnotations) {
            fillAutomationPackageWithAnnotatedKeywordsAndPlans(archive, isLocalPackage, res);
        }

        // apply imported fragments recursively
        if (descriptor != null) {
            readAutomationPackageYamlFragmentTree(archive, descriptor);
            fillAutomationPackageWithImportedFragments(res, descriptor, archive);
        }
        return res;
    }


    private String resolveName(AutomationPackageDescriptorYaml descriptor, T archive) throws AutomationPackageReadingException {
        String finalName;
        if (descriptor != null) {
            finalName = descriptor.getName();
        } else {
            finalName = Objects.requireNonNullElse(archive.getAutomationPackageName(), "local-automation-package");
        }
        return validatePackageName(finalName);
    }

    /**
     * Validates and normalizes package name to ensure it's safe for use in Groovy expressions.
     *
     * @param name The package name to validate
     * @return A normalized, safe package name
     * @throws IllegalArgumentException if the name cannot be normalized safely
     */
    private String validatePackageName(String name) throws AutomationPackageReadingException {
        if (name == null || name.trim().isEmpty()) {
            throw new AutomationPackageReadingException("Package name cannot be null or empty");
        }
        // Check for characters that could break Groovy expressions
        if (name.contains("'") || name.contains("\\")) {
            throw new AutomationPackageReadingException(
                    "Package name contains unsafe characters: " + name +
                            ". Simple quote and backslash characters are not allowed."
            );
        }
        return name;
    }

    private String resolveUniqueName(String baseName, String apVersion) {
        String finalName = baseName;
        if (apVersion != null && !apVersion.isEmpty()) {
            finalName += AP_VERSION_SEPARATOR;
            finalName += apVersion;
        }
        return finalName;
    }

    protected AutomationPackageContent newContentInstance(){
        return new AutomationPackageContent();
    }

    abstract protected void fillAutomationPackageWithAnnotatedKeywordsAndPlans(T archive, boolean isLocalPackage, AutomationPackageContent res) throws AutomationPackageReadingException;


    public AutomationPackageYamlFragmentManager provideAutomationPackageYamlFragmentManager(T archive) throws AutomationPackageReadingException {
        AutomationPackageDescriptorReader reader = getOrCreateDescriptorReader();
        AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(archive.getDescriptorYamlUrl(), archive.getOriginalFileName());
        readAutomationPackageYamlFragmentTree(archive, descriptor);
        return new AutomationPackageYamlFragmentManager(descriptor, getOrCreateDescriptorReader());
    }

    private void readAutomationPackageYamlFragmentTree(AutomationPackageArchive archive, AutomationPackageFragmentYaml parent) throws AutomationPackageReadingException {

        if (!parent.getFragments().isEmpty()) {
            for (String importedFragmentReference : parent.getFragments()) {
                List<URL> resources = archive.getResourcesByPattern(importedFragmentReference);
                for (URL resource : resources) {
                    AutomationPackageFragmentYaml fragment = getOrCreateDescriptorReader().readAutomationPackageFragment(resource, archive.getOriginalFileName());
                    fragment.setParent(parent);
                    parent.getChildren().add(fragment);
                    readAutomationPackageYamlFragmentTree(archive, fragment);
                }
            }
        }
    }

    private void fillAutomationPackageWithImportedFragments(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment, T archive) throws AutomationPackageReadingException {
        fillContentSections(targetPackage, fragment, archive);

        for (AutomationPackageFragmentYaml child: fragment.getChildren()) {
            fillAutomationPackageWithImportedFragments(targetPackage, child, archive);
        }
    }

    protected void fillContentSections(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment, T archive) throws AutomationPackageReadingException {
        targetPackage.getKeywords().addAll(fragment.getKeywords());
        targetPackage.getPlans().addAll(fragment.getPlans().stream().map(p -> {
            Plan plan = getOrCreateDescriptorReader().getPlanReader().yamlPlanToPlan(p);
            plan.getAttributes().put("fragmentUrl", fragment.getFragmentUrl().toString());
            plan.getAttributes().put("nameInYaml", p.getName());
            return plan;
        }).collect(Collectors.toList()));

        readPlainTextPlans(targetPackage, fragment, archive);

        for (Map.Entry<String, List<?>> additionalField : fragment.getAdditionalFields().entrySet()) {
            boolean hooked = hookRegistry.onAdditionalDataRead(additionalField.getKey(), additionalField.getValue(), targetPackage);
            if (!hooked) {
                log.warn("Hook not found for additional field " + additionalField.getKey() + ". The additional field has been skipped");
            }
        }
    }

    private void readPlainTextPlans(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment, T archive) throws AutomationPackageReadingException {
        // parse plain - text plans
        for (YamlPlainTextPlan plainTextPlan : fragment.getPlansPlainText()) {
            try {
                List<URL> urls;
                boolean wildcard = false;
                if (ResourcePathMatchingResolver.containsWildcard(plainTextPlan.getFile())) {
                    wildcard = true;
                    ResourcePathMatchingResolver resourceResolver = archive.getResourcePathMatchingResolver();
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
