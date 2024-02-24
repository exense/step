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

import step.automation.packages.*;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.functions.Function;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.junit.runner.StepClassParser;
import step.resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class StepJarParser {

    private final AbstractAutomationPackageReader<?> automationPackageReader;
    private final AutomationPackageKeywordsAttributesApplier automationPackagesKeywordAttributesApplier;
    private final StepClassParser stepClassParser;

    public StepJarParser() {
        this(null, null);
    }

    public StepJarParser(ResourceManager resourceManager, AbstractAutomationPackageReader<?> automationPackageReader) {
        this.stepClassParser = new StepClassParser(false);
        this.automationPackageReader = automationPackageReader;
        this.automationPackagesKeywordAttributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);
    }

    private List<Function> getFunctions(AnnotationScanner annotationScanner, File artifact, File libraries) {
        try {
            // reuse logic from automation package reader to scan annotated keywords
            // BUT in contrast with automation package here we need to fill scriptFile and librariesFile references immediately
            List<Function> functions = AbstractAutomationPackageReader
                    .extractAnnotatedKeywords(annotationScanner, false, artifact.getAbsolutePath(), libraries != null ? libraries.getAbsolutePath() : null)
                    .stream()
                    .map(AutomationPackageKeyword::toDraftKeyword)
                    .collect(Collectors.toList());

            // if artifact is an automation package we need to add keywords from yaml descriptors (annotated keywords have already been included above)
            try (AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(artifact, artifact.getName())) {
                // add functions from automation package
                if (automationPackageArchive.hasAutomationPackageDescriptor() && automationPackageReader != null) {
                    AutomationPackageContent content = automationPackageReader.readAutomationPackage(automationPackageArchive, false, false);
                    functions.addAll(automationPackagesKeywordAttributesApplier.applySpecialAttributesToKeyword(
                            content.getKeywords(), automationPackageArchive, null)
                    );
                }
            }
            return functions;
        } catch (AutomationPackageReadingException | JsonSchemaPreparationException | IOException e) {
            throw new RuntimeException("Unable to process automation package", e);
        }
    }

    public PlansParsingResult getPlansForJar(File artifact, File dependency, String[] includedClasses, String[] includedAnnotations,
                                             String[] excludedClasses, String[] excludedAnnotations) {
        try (AnnotationScanner annotationScanner = AnnotationScanner.forSpecificJar(artifact)) {
            // This code is moved to automation package reader just to be reused in StepJarParser for now.
            // Further the StepJarParser should be completely replaced with automation package functionality
            AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(artifact, artifact.getName());
            List<Plan> result = AbstractAutomationPackageReader.extractAnnotatedPlans(
                    automationPackageArchive, annotationScanner, includedClasses, includedAnnotations, excludedClasses, excludedAnnotations, stepClassParser
            );

            // Find all keywords
            List<Function> functions = getFunctions(annotationScanner, artifact, dependency);

            return new PlansParsingResult(result, functions);
        } catch (Exception e) {
            throw new RuntimeException("Exception when trying to list the plans of jar file '" + artifact.getName() + "'", e);
        }
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

}
