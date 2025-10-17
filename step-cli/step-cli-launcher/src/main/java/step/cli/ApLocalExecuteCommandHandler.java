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
package step.cli;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.*;
import step.automation.packages.junit.AbstractLocalPlanRunner;
import step.automation.packages.library.AutomationPackageLibraryProvider;
import step.automation.packages.library.AutomationPackageLibraryFromInputStreamProvider;
import step.automation.packages.library.NoAutomationPackageLibraryProvider;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.junit.runner.StepClassParserResult;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static step.cli.ExecuteAutomationPackageTool.getPlanFilters;

public class ApLocalExecuteCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ApLocalExecuteCommandHandler.class);

    public void execute(File apFile, File kwLibFile, String includePlans, String excludePlans, String includeCategories,
                        String excludeCategories, Map<String, String> executionParameters) throws StepCliExecutionException {
        try (ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new AbstractExecutionEnginePlugin() {
            @Override
            public void afterExecutionEnd(ExecutionContext context) {
                super.afterExecutionEnd(context);
            }
        }).withPluginsFromClasspath().build()) {
            AutomationPackageManager automationPackageManager = executionEngine.getExecutionEngineContext().require(AutomationPackageManager.class);

            InputStream kwFileInputStream = null;
            try (InputStream is = new FileInputStream(apFile)) {
                if (kwLibFile != null) {
                    kwFileInputStream = new FileInputStream(kwLibFile);
                }
                AutomationPackageLibraryProvider kwFromInputStreamProvider = kwFileInputStream == null ?
                        new NoAutomationPackageLibraryProvider() :
                        new AutomationPackageLibraryFromInputStreamProvider(kwFileInputStream, kwLibFile.getName());
                AutomationPackageFromInputStreamProvider automationPackageProvider = new AutomationPackageFromInputStreamProvider(automationPackageManager.getAutomationPackageReaderRegistry(),
                        is, apFile.getName(), kwFromInputStreamProvider);
                AutomationPackageUpdateParameter localCreateParameters = new AutomationPackageUpdateParameterBuilder().withCreateOnly()
                        .withLocalOnly().build();
                ObjectId automationPackageId = automationPackageManager.createOrUpdateAutomationPackage(
                        automationPackageProvider, kwFromInputStreamProvider, localCreateParameters).getId();

                PlanFilter planFilters = getPlanFilters(includePlans, excludePlans, includeCategories, excludeCategories);
                List<StepClassParserResult> listPlans = automationPackageManager.getPackagePlans(automationPackageId)
                        .stream()
                        .filter(planFilters::isSelected)
                        .filter(p -> p.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution())
                        .map(p -> new StepClassParserResult(getPlanName(p), p, null))
                        .collect(Collectors.toList());

                log.info("The following plans will be executed: {}", listPlans.stream().map(StepClassParserResult::getName).collect(Collectors.toList()));

                for (StepClassParserResult parserResult : listPlans) {
                    new AbstractLocalPlanRunner(parserResult, executionEngine) {
                        @Override
                        protected void onExecutionStart() {
                            log.info("Execution has been started for plan {}", parserResult.getName());
                        }

                        @Override
                        protected void onExecutionError(PlanRunnerResult result, String errorText, boolean assertionError) {
                            log.error("Execution has been failed for plan {}. {}", parserResult.getName(), errorText);

                            String executionTree = ExecuteAutomationPackageTool.getExecutionTreeAsString(result);
                            String detailMessage = errorText + "\n" + executionTree;
                            if(assertionError){
                                detailMessage += "Assertion error. ";
                            }
                            detailMessage += "Execution tree is: " + executionTree;
                            log.error(detailMessage);
                        }

                        @Override
                        protected void onInitializingException(Exception exception) {
                            log.error("Execution initialization exception for plan {}.", parserResult.getName(), exception);
                        }

                        @Override
                        protected void onExecutionException(Exception exception) {
                            log.error("Execution exception for plan {}", parserResult.getName(), exception);
                        }

                        @Override
                        protected void onTestFinished() {
                            log.info("Execution has been finished for plan {}", parserResult.getName());
                        }

                        @Override
                        protected Map<String, String> getExecutionParameters() {
                            return executionParameters;
                        }
                    }.runPlan();
                }
            } catch (FileNotFoundException e) {
                throw new StepCliExecutionException("File not found: " + apFile.getAbsolutePath(), e);
            } catch (IOException e) {
                throw new RuntimeException("IO exception for " + apFile.getAbsolutePath(), e);
            } catch (AutomationPackageReadingException e) {
                throw new RuntimeException("AP reading exception", e);
            } finally {
                if (kwFileInputStream != null) {
                    try {
                        kwFileInputStream.close();
                    } catch (IOException e) {
                        log.error("Input stream for KW file cannot be closed", e);
                    }
                }
            }
        }
    }

    private static String getPlanName(Plan p) {
        return p.getAttribute(AbstractOrganizableObject.NAME);
    }

    private List<String> parseList(String string) {
        return (string == null || string.isEmpty()) ? new ArrayList<>() : Arrays.stream(string.split(",")).collect(Collectors.toList());
    }
}
