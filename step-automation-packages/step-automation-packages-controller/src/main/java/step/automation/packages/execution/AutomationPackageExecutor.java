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
import step.automation.packages.AutomationPackageManager;
import step.core.artefacts.Artefact;
import step.core.execution.model.*;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.ExecutionScheduler;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AutomationPackageExecutor {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageExecutor.class);

    // TODO: timeouts?
    private static final int AUTOMATION_PACKAGE_EXECUTION_TIMEOUT = 60 * 60 * 1000;
    private static final int CLEANUP_POLLING_INTERVAL = 5000;

    private final ExecutorService delayedCleanupExecutor = Executors.newFixedThreadPool(5);

    private final ExecutionScheduler scheduler;
    private final ExecutionAccessor executionAccessor;
    private final FunctionTypeRegistry functionTypeRegistry;
    private final FunctionAccessor functionAccessor;
    private final IsolatedAutomationPackageRepository isolatedAutomationPackageRepository;
    private final AutomationPackageManager automationPackageManager;

    public AutomationPackageExecutor(ExecutionScheduler scheduler,
                                     ExecutionAccessor executionAccessor,
                                     FunctionTypeRegistry functionTypeRegistry,
                                     FunctionAccessor functionAccessor,
                                     IsolatedAutomationPackageRepository isolatedAutomationPackageRepository,
                                     AutomationPackageManager automationPackageManager) {
        this.scheduler = scheduler;
        this.executionAccessor = executionAccessor;
        this.functionTypeRegistry = functionTypeRegistry;
        this.functionAccessor = functionAccessor;
        this.isolatedAutomationPackageRepository = isolatedAutomationPackageRepository;
        this.automationPackageManager = automationPackageManager;
    }

    public List<String> runInIsolation(InputStream automationPackage, String fileName, AutomationPackageExecutionParameters parameters,
                                       ObjectEnricher objectEnricher, ObjectPredicate objectPredicate) {
        ObjectId contextId = new ObjectId();

        // prepare the isolated in-memory automation package manager with the only one automation package
        AutomationPackageManager inMemoryPackageManager = automationPackageManager.createIsolated(
                contextId, functionTypeRegistry,
                functionAccessor
        );

        List<String> executions = new ArrayList<>();
        try {
            ObjectId packageId = inMemoryPackageManager.createAutomationPackage(automationPackage, fileName, objectEnricher, objectPredicate);

            isolatedAutomationPackageRepository.putContext(contextId.toString(), inMemoryPackageManager);

            for (Plan plan : inMemoryPackageManager.getPackagePlans(packageId)) {
                PlanFilter planFilter = parameters.getPlanFilter();

                if ((planFilter == null || planFilter.isSelected(plan)) && plan.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution()) {
                    ExecutionParameters params = parameters.toExecutionParameters();
                    params.setIsolatedExecution(true);
                    HashMap<String, String> repositoryParameters = new HashMap<>();
                    repositoryParameters.put(IsolatedAutomationPackageRepository.REPOSITORY_PARAM_CONTEXTID, contextId.toString());
                    repositoryParameters.put(RepositoryObjectReference.PLAN_ID, plan.getId().toString());

                    params.setRepositoryObject(new RepositoryObjectReference(IsolatedAutomationPackageRepositoryPlugin.ISOLATED_AUTOMATION_PACKAGE, repositoryParameters));
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
            cleanupIsolatedContextAfterExecution(contextId, executions, fileName);
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

    protected void cleanupIsolatedContextAfterExecution(ObjectId contextId, List<String> executions, String fileName) {
        // wait for all executions to be finished
        delayedCleanupExecutor.execute(() -> {
            waitForAllExecutionEnded(executions);

            // remove the context from isolated automation package repository
            log.info("Cleanup isolated execution context");
            isolatedAutomationPackageRepository.cleanupContext(contextId.toString());

            log.info("Execution finished for automation package {}", fileName);
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
