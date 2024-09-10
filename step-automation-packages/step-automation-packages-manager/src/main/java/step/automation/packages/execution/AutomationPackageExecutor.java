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
package step.automation.packages.execution;

import ch.exense.commons.io.Poller;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackage;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.model.*;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;
import step.core.repositories.RepositoryObjectReference;
import step.resources.Resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AutomationPackageExecutor {

    public static final String ISOLATED_AUTOMATION_PACKAGE = "isolatedAutomationPackage";
    private static final Logger log = LoggerFactory.getLogger(AutomationPackageExecutor.class);

    // TODO: timeouts?
    private static final int AUTOMATION_PACKAGE_EXECUTION_TIMEOUT = 60 * 60 * 1000;
    private static final int CLEANUP_POLLING_INTERVAL = 5000;

    private final ExecutorService delayedCleanupExecutor = Executors.newFixedThreadPool(5);

    private final ExecutionLauncher scheduler;
    private final ExecutionAccessor executionAccessor;
    private final IsolatedAutomationPackageRepository isolatedAutomationPackageRepository;

    public AutomationPackageExecutor(ExecutionLauncher scheduler,
                                     ExecutionAccessor executionAccessor,
                                     IsolatedAutomationPackageRepository isolatedAutomationPackageRepository) {
        this.scheduler = scheduler;
        this.executionAccessor = executionAccessor;
        this.isolatedAutomationPackageRepository = isolatedAutomationPackageRepository;
    }

    public List<String> runInIsolation(InputStream apInputStream, String fileName, AutomationPackageExecutionParameters parameters,
                                       ObjectEnricher objectEnricher, ObjectPredicate objectPredicate) {

        ObjectId contextId = new ObjectId();
        List<String> executions = new ArrayList<>();

        // here we need to read the input stream twice:
        // 1) to store the original file into the isolatedAutomationPackageRepository and support re-execution
        // 2) to read the automation package and fill ap manager with plans, keywords etc.

        // so at first we store the input stream as resource
        Resource apResource = isolatedAutomationPackageRepository.saveApResource(contextId.toString(), apInputStream, fileName);

        // and then we read the ap from just stored file
        try (FileInputStream fis = new FileInputStream(isolatedAutomationPackageRepository.getApFile(apResource))) {
            // create single execution context for the whole AP to execute all plans on the same ap manager (for performance reason)
            IsolatedAutomationPackageRepository.PackageExecutionContext executionContext =
                    isolatedAutomationPackageRepository.createPackageExecutionContext(contextId.toString(), fis, fileName, objectEnricher, objectPredicate);
            try {
                AutomationPackage automationPackage = executionContext.getAutomationPackage();
                String apName = automationPackage.getAttribute(AbstractOrganizableObject.NAME);

                // we have resolved the name of ap, and we need to save this name as custom field in resource to look up this resource during re-execution
                isolatedAutomationPackageRepository.setApNameForResource(apResource, apName);

                for (Plan plan : executionContext.getInMemoryManager().getPackagePlans(automationPackage.getId())) {
                    PlanFilter planFilter = parameters.getPlanFilter();

                    if ((planFilter == null || planFilter.isSelected(plan)) && plan.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution()) {
                        ExecutionParameters params = parameters.toExecutionParameters();
                        params.setPlan(plan);
                        params.setIsolatedExecution(true);
                        HashMap<String, String> repositoryParameters = new HashMap<>();

                        // save apName + contextId + planName to support re-execution
                        repositoryParameters.put(IsolatedAutomationPackageRepository.AP_NAME, apName);
                        repositoryParameters.put(IsolatedAutomationPackageRepository.REPOSITORY_PARAM_CONTEXTID, contextId.toString());
                        repositoryParameters.put(IsolatedAutomationPackageRepository.PLAN_NAME, plan.getAttribute(AbstractOrganizableObject.NAME));

                        params.setRepositoryObject(new RepositoryObjectReference(ISOLATED_AUTOMATION_PACKAGE, repositoryParameters));
                        params.setDescription(CommonExecutionParameters.defaultDescription(plan));

                        // for instance, set the project for multitenant application
                        if (objectEnricher != null) {
                            objectEnricher.accept(params);
                        }

                        String newExecutionId = this.scheduler.execute(params);
                        if (newExecutionId != null) {
                            executions.add(newExecutionId);
                        }
                    }
                }
            } finally {
                // after all plans are executed we can clean up the context (remove temporary files prepared for isolated execution)
                waitForAllLaunchedExecutions(executions, fileName, executionContext);
            }
        } catch (IOException e) {
            String msg = "Unable to read stored automation package file " + fileName;
            log.error(msg);
            throw new RuntimeException(msg, e);
        }
        return executions;
    }

    public void shutdown() throws InterruptedException {
        this.delayedCleanupExecutor.shutdown();
        boolean terminated = this.delayedCleanupExecutor.awaitTermination(60, TimeUnit.SECONDS);
        if (!terminated) {
            log.warn("Unable to terminate the execution cleanup");
        }
    }

    protected void waitForAllLaunchedExecutions(List<String> executions, String fileName, IsolatedAutomationPackageRepository.PackageExecutionContext executionContext) {
        // wait for all executions to be finished
        delayedCleanupExecutor.execute(() -> {
            waitForAllExecutionEnded(executions);

            log.info("Execution finished for automation package {}", fileName);
            try {
                executionContext.close();
            } catch (IOException e) {
                log.error("Unable to close the execution context for automation package " + fileName);
            }
        });

    }

    private boolean waitForAllExecutionEnded(List<String> executions) {
        try {
            Poller.waitFor(() -> {
                // continue polling until all executions in current context are ended
                List<Execution> currentExecutions = executionAccessor.findByIds(executions).collect(Collectors.toList());
                boolean activeExecutionFound = false;
                boolean executionFound = false;
                for (String executionId : executions) {
                    for (Execution currentExecution : currentExecutions) {
                        if (currentExecution.getId().toString().equals(executionId)) {
                            if (!currentExecution.getStatus().equals(ExecutionStatus.ENDED)) {
                                activeExecutionFound = true;
                            }
                            executionFound = true;
                            break;
                        }
                    }

                    if (!executionFound) {
                        // unexpected situation - execution accessor didn't return launched execution
                        throw new RuntimeException("Execution not found by id: " + executionId);
                    }

                    if (activeExecutionFound) {
                        break;
                    }
                }

                return !activeExecutionFound;
            }, AUTOMATION_PACKAGE_EXECUTION_TIMEOUT, CLEANUP_POLLING_INTERVAL);
        } catch (InterruptedException e) {
            log.warn("Isolated execution interrupted");
            return false;
        } catch (Throwable e) {
            log.error("Exception during isolated execution", e);
            return false;
        }
        return true;
    }

}
