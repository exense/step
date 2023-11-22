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
import step.automation.packages.accessor.InMemoryAutomationPackageAccessorImpl;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.functions.accessor.FunctionAccessor;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.resources.LocalResourceManagerImpl;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AutomationPackageExecutor {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageExecutor.class);

    private final ExecutorService delayedCleanupExecutor = Executors.newFixedThreadPool(5);

    private final ExecutionScheduler scheduler;
    private final FunctionManager layeredFunctionManager;
    private final FunctionAccessor layeredFunctionAccessor;
    private final IsolatedAutomationPackageRepository isolatedAutomationPackageRepository;

    public AutomationPackageExecutor(ExecutionScheduler scheduler,
                                     FunctionManager layeredFunctionManager,
                                     FunctionAccessor layeredFunctionAccessor,
                                     IsolatedAutomationPackageRepository isolatedAutomationPackageRepository) {
        this.scheduler = scheduler;
        this.layeredFunctionManager = layeredFunctionManager;
        this.layeredFunctionAccessor = layeredFunctionAccessor;
        this.isolatedAutomationPackageRepository = isolatedAutomationPackageRepository;
    }

    public List<String> runInIsolation(InputStream automationPackage, String fileName, Map<String, String> executionParameters, ObjectEnricher objectEnricher, String userId) throws SetupFunctionException, FunctionTypeException {
        ObjectId contextId = new ObjectId();

        // prepare the isolated in-memory automation package manager with the only one automation package
        AutomationPackageManager inMemoryPackageManager = createInMemoryAutomationPackageManager(contextId);
        ObjectId packageId = inMemoryPackageManager.createAutomationPackage(automationPackage, fileName, objectEnricher);

        List<String> executions = new ArrayList<>();
        try {
            isolatedAutomationPackageRepository.putContext(contextId.toString(), inMemoryPackageManager);

            // TODO: filter plans
            for (Plan plan : inMemoryPackageManager.getPackagePlans(packageId)) {
                ExecutionParameters params = new ExecutionParameters();
                HashMap<String, String> repositoryParameters = new HashMap<>();
                repositoryParameters.put(IsolatedAutomationPackageRepository.REPOSITORY_PARAM_CONTEXTID, contextId.toString());
                repositoryParameters.put(RepositoryObjectReference.PLAN_ID, plan.getId().toString());

                params.setRepositoryObject(new RepositoryObjectReference(IsolatedAutomationPackageRepositoryPlugin.ISOLATED_AUTOMATION_PACKAGE, repositoryParameters));
                params.setMode(ExecutionMode.RUN);
                params.setDescription(plan.getRoot().getAttributes().get("name"));

                if (userId != null) {
                    params.setUserID(userId);
                }
                params.setCustomParameters(executionParameters);

                String newExecutionId = this.scheduler.execute(params);
                if (newExecutionId != null) {
                    executions.add(newExecutionId);
                }
            }
        } finally {
            cleanupContextAfterAllExecutions(contextId, inMemoryPackageManager, executions, scheduler);
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

    protected void cleanupContextAfterAllExecutions(ObjectId contextId, AutomationPackageManager packageManager, List<String> executions, ExecutionScheduler scheduler) {
        // wait for all executions to be finished
        delayedCleanupExecutor.execute(() -> {
            // TODO: timeouts ?
            try {
                Poller.waitFor(() -> {
                    // continue polling until all executions in current context are ended
                    List<ExecutionContext> currentExecutions = scheduler.getCurrentExecutions();
                    boolean activeExecutionFound = false;
                    for (String executionId : executions) {
                        for (ExecutionContext currentExecution : currentExecutions) {
                            if (currentExecution.getExecutionId().equals(executionId)) {
                                if (!currentExecution.getStatus().equals(ExecutionStatus.ENDED)) {
                                    activeExecutionFound = true;
                                }
                                break;
                            }
                        }
                        if (activeExecutionFound) {
                            break;
                        }
                    }

                    return !activeExecutionFound;
                }, 60 * 60 * 1000, 5000);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();  //set the flag back to true
                return;
            } catch (Throwable e) {
                log.error("Exception during execution cleanup", e);
                return;
            }

            // remove stored resources
            packageManager.getResourceManager().cleanup();

            // remove the context from isolated automation package repository
            isolatedAutomationPackageRepository.removeContext(contextId.toString());
        });

    }

    private AutomationPackageManager createInMemoryAutomationPackageManager(ObjectId contextId) {
        return new AutomationPackageManager(
                new InMemoryAutomationPackageAccessorImpl(),
                layeredFunctionManager,
                layeredFunctionAccessor,
                new InMemoryPlanAccessor(),
                new LocalResourceManagerImpl(new File("resources", contextId.toString())),
                new InMemoryExecutionTaskAccessor(),
                null
        );
    }
}
