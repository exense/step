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

import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageDescriptorReader;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.scanner.CachedAnnotationScanner;
import step.engine.plugins.LocalFunctionPlugin;
import step.functions.Function;
import step.handlers.javahandler.Keyword;
import step.plans.nl.parser.PlanParser;
import step.plugins.functions.types.CompositeFunctionUtils;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.automation.GeneralScriptFunctionConversionRule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AutomationPackageReader {

    private final AutomationPackageDescriptorReader descriptorReader;

    public AutomationPackageReader(String jsonSchema) {
        this.descriptorReader = new AutomationPackageDescriptorReader(jsonSchema);
    }

    public AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive, boolean isLocalPackage) throws AutomationPackageReadingException {
        try {
            if (!automationPackageArchive.isAutomationPackage()) {
                return null;
            }

            try (InputStream yamlInputStream = automationPackageArchive.getDescriptorYaml()) {
                AutomationPackageDescriptorYaml descriptorYaml = descriptorReader.readAutomationPackageDescriptor(yamlInputStream);
                return buildAutomationPackage(descriptorYaml, automationPackageArchive, isLocalPackage);
            }
        } catch (IOException ex) {
            throw new AutomationPackageReadingException("Unable to read the automation package", ex);
        }
    }

    protected AutomationPackageContent buildAutomationPackage(AutomationPackageDescriptorYaml descriptor, AutomationPackageArchive archive, boolean isLocalPackage) throws AutomationPackageReadingException {
        AutomationPackageContent res = new AutomationPackageContent();
        res.setName(descriptor.getName());

        fillAutomationPackageWithAnnotatedKeywords(res, archive, isLocalPackage);

        // apply imported fragments recursively
        fillAutomationPackageWithImportedFragments(res, descriptor, archive);
        return res;
    }

    private void fillAutomationPackageWithAnnotatedKeywords(AutomationPackageContent res, AutomationPackageArchive archive, boolean isLocalPackage) {
        // TODO: avoid duplication with StepJarParser
        Set<Method> methods = CachedAnnotationScanner.getMethodsWithAnnotation(Keyword.class, archive.getClassLoader());

        if (isLocalPackage) {
            List<Function> functions = LocalFunctionPlugin.getLocalFunctions(methods);
            for (Function f : functions) {
                res.getKeywords().add(new AutomationPackageKeyword(f, new HashMap<>()));
            }
            // TODO: composite functions
        } else {
            for (Method m : methods) {
                Keyword annotation = m.getAnnotation(Keyword.class);
                Function f;
                if (annotation.planReference() != null && !annotation.planReference().isBlank()) {
                    try {
                        f = CompositeFunctionUtils.createCompositeFunction(
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

                    // to be filled by AutomationPackageKeywordsAttributesApplier
                    function.setScriptFile(new DynamicValue<>(""));

                    // TODO: libraries?
//                if (libraries != null) {
//                    function.setLibrariesFile(new DynamicValue<>(libraries.getAbsolutePath()));
//                }
                    function.getCallTimeout().setValue(annotation.timeout());
                    function.setDescription(annotation.description());
                    function.setScriptLanguage(new DynamicValue<>("java"));
                    f = function;
                }

                res.getKeywords().add(new AutomationPackageKeyword(f, Map.of(GeneralScriptFunctionConversionRule.AUTOMATION_PACKAGE_FILE_REFERENCE, "")));
            }
        }
    }

    public void fillAutomationPackageWithImportedFragments(AutomationPackageContent targetPackage, AutomationPackageFragmentYaml fragment, AutomationPackageArchive archive) throws AutomationPackageReadingException {
        targetPackage.getKeywords().addAll(fragment.getKeywords());
        targetPackage.getPlans().addAll(fragment.getPlans().stream().map(p -> descriptorReader.getPlanReader().yamlPlanToPlan(p)).collect(Collectors.toList()));
        targetPackage.getSchedules().addAll(fragment.getSchedules());

        if (!fragment.getFragments().isEmpty()) {
            for (String importedFragmentReference : fragment.getFragments()) {
                try (InputStream fragmentYamlStream = archive.getResourceAsStream(importedFragmentReference)) {
                    fragment = descriptorReader.readAutomationPackageFragment(fragmentYamlStream, importedFragmentReference);
                    fillAutomationPackageWithImportedFragments(targetPackage, fragment, archive);
                } catch (IOException e) {
                    throw new AutomationPackageReadingException("Unable to read fragment in automation package: " + importedFragmentReference, e);
                }
            }
        }
    }

    public AutomationPackageContent readAutomationPackageFromJarFile(File automationPackageJar) throws AutomationPackageReadingException {
        try (AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(automationPackageJar)) {
            return readAutomationPackage(automationPackageArchive, false);
        } catch (IOException e) {
            throw new AutomationPackageReadingException("IO Exception", e);
        }
    }
}
