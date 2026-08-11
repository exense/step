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
import step.cli.local.LocalAgentProvisioningConfiguration;
import step.cli.local.LocalAgentProvisioningPlugin;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.ExecutionEngine;
import step.core.execution.OperationMode;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;
import step.core.plans.runner.PlanRunnerResult;
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

    public void execute(File apFile, File libFile, String includePlans, String excludePlans, String includeCategories,
                        String excludeCategories, Map<String, String> executionParameters,
                        LocalAgentProvisioningConfiguration localAgentConfiguration) throws StepCliExecutionException {
        // The keywords run on real agents started as separate processes on this machine, so that a local execution
        // exercises the same agents and the same class loader isolation as an execution on a Step platform, and
        // supports every keyword language rather than only the Java ones.
        try (ExecutionEngine executionEngine = ExecutionEngine.builder()
            .withOperationMode(OperationMode.LOCAL_AUTOMATION_PACKAGE_WITH_AGENTS)
            .withPlugin(new LocalAgentProvisioningPlugin(localAgentConfiguration))
            .withPluginsFromClasspath().build()) {
            AutomationPackageManager automationPackageManager = executionEngine.getExecutionEngineContext().require(AutomationPackageManager.class);

            InputStream libFileInputStream = null;
            try (InputStream is = new FileInputStream(apFile)) {
                if (libFile != null) {
                    libFileInputStream = new FileInputStream(libFile);
                }
                AutomationPackageLibraryProvider libFromInputStreamProvider = libFileInputStream == null ?
                    new NoAutomationPackageLibraryProvider() :
                    new AutomationPackageLibraryFromInputStreamProvider(libFileInputStream, libFile.getName());
                AutomationPackageFromInputStreamProvider automationPackageProvider = new AutomationPackageFromInputStreamProvider(automationPackageManager.getAutomationPackageReaderRegistry(),
                    is, apFile.getName(), libFromInputStreamProvider);
                AutomationPackageUpdateParameter localCreateParameters = new AutomationPackageUpdateParameterBuilder().withCreateOnly()
                    .forLocalExecution().build();
                ObjectId automationPackageId = automationPackageManager.createOrUpdateAutomationPackage(
                    automationPackageProvider, libFromInputStreamProvider, localCreateParameters).getId();

                PlanFilter planFilters = getPlanFilters(includePlans, excludePlans, includeCategories, excludeCategories);
                List<StepClassParserResult> listPlans = automationPackageManager.getPackagePlans(automationPackageId)
                    .stream()
                    .filter(planFilters::isSelected)
                    .filter(p -> p.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution())
                    .map(p -> new StepClassParserResult(getPlanName(p), p, null))
                    .collect(Collectors.toList());

                log.info("The following plans will be executed: {}", listPlans.stream().map(StepClassParserResult::getName).collect(Collectors.toList()));

                List<String> failedPlans = new ArrayList<>();
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
                            if (assertionError) {
                                detailMessage += "Assertion error. ";
                            }
                            detailMessage += "Execution tree is: " + executionTree;
                            log.error(detailMessage);
                            failedPlans.add(parserResult.getName());
                        }

                        @Override
                        protected void onInitializingException(Exception exception) {
                            log.error("Execution initialization exception for plan {}.", parserResult.getName(), exception);
                            failedPlans.add(parserResult.getName());
                        }

                        @Override
                        protected void onExecutionException(Exception exception) {
                            log.error("Execution exception for plan {}", parserResult.getName(), exception);
                            failedPlans.add(parserResult.getName());
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

                // The details of each failure have been logged above. Reporting them here as well is what makes the
                // command exit with an error: a local execution used in a pipeline has to fail the build when a plan
                // fails, exactly like the remote one does.
                if (!failedPlans.isEmpty()) {
                    throw new StepCliExecutionException(failedPlans.size() + "/" + listPlans.size()
                        + " plan(s) failed: " + failedPlans);
                }
            } catch (FileNotFoundException e) {
                throw new StepCliExecutionException("File not found: " + apFile.getAbsolutePath(), e);
            } catch (IOException e) {
                throw new RuntimeException("IO exception for " + apFile.getAbsolutePath(), e);
            } catch (AutomationPackageReadingException e) {
                throw new RuntimeException("AP reading exception", e);
            } finally {
                if (libFileInputStream != null) {
                    try {
                        libFileInputStream.close();
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
