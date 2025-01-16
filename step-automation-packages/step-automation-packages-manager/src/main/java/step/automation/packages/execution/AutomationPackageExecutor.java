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
import step.automation.packages.AutomationPackageManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.model.*;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;
import step.core.repositories.RepositoryObjectManager;
import step.core.repositories.RepositoryObjectReference;
import step.repositories.ArtifactRepositoryConstants;

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
    public static final String LOCAL_AUTOMATION_PACKAGE = "localAutomationPackage";
    private static final Logger log = LoggerFactory.getLogger(AutomationPackageExecutor.class);

    private static final int CLEANUP_POLLING_INTERVAL = 5000;

    private final ExecutorService delayedCleanupExecutor = Executors.newFixedThreadPool(5);

    private final AutomationPackageManager mainAutomationPackageManager;
    private final ExecutionLauncher scheduler;
    private final ExecutionAccessor executionAccessor;
    private final RepositoryObjectManager repositoryObjectManager;
    private final Integer isolatedExecutionTimeout;

    public AutomationPackageExecutor(AutomationPackageManager mainAutomationPackageManager,
                                     ExecutionLauncher scheduler,
                                     ExecutionAccessor executionAccessor,
                                     RepositoryObjectManager repositoryObjectManager, int isolatedExecutionTimeout) {
        this.mainAutomationPackageManager = mainAutomationPackageManager;
        this.scheduler = scheduler;
        this.executionAccessor = executionAccessor;
        this.repositoryObjectManager = repositoryObjectManager;
        this.isolatedExecutionTimeout = isolatedExecutionTimeout;
    }

    /**
     * Runs plans from automation package already deployed in Step (existing in DB)
     * @return the ids of launched executions
     */
    public List<String> runDeployedAutomationPackage(ObjectId automationPackageId,
                                                     AutomationPackageExecutionParameters parameters,
                                                     ObjectEnricher objectEnricher,
                                                     ObjectPredicate objectPredicate){
        // throws an exception if ap doesn't exist
        AutomationPackage automationPackage = mainAutomationPackageManager.getAutomatonPackageById(automationPackageId, objectPredicate);

        return runExecutions(automationPackage, LOCAL_AUTOMATION_PACKAGE, null, null, mainAutomationPackageManager, parameters, objectEnricher);
    }

    public List<String> runInIsolation(InputStream apInputStream, String inputStreamFileName, IsolatedAutomationPackageExecutionParameters parameters,
                                       ObjectEnricher objectEnricher, ObjectPredicate objectPredicate) {

        ObjectId contextId = new ObjectId();
        List<String> executions = new ArrayList<>();

        String repoId = parameters.getOriginalRepositoryObject() != null ? parameters.getOriginalRepositoryObject().getRepositoryID() : ISOLATED_AUTOMATION_PACKAGE;
        RepositoryWithAutomationPackageSupport repository = (RepositoryWithAutomationPackageSupport) repositoryObjectManager.getRepository(repoId);

        // here we need to read the input stream twice:
        // 1) to store the original file into the isolatedAutomationPackageRepository and support re-execution
        // 2) to read the automation package and fill ap manager with plans, keywords etc.

        // so at first we store the input stream as resource
        IsolatedAutomationPackageRepository.AutomationPackageFile apFile = repository.getApFileForExecution(apInputStream, inputStreamFileName, parameters, contextId);

        // and then we read the ap from just stored file
        // create single execution context for the whole AP to execute all plans on the same ap manager (for performance reason)
        IsolatedAutomationPackageRepository.PackageExecutionContext executionContext = repository.createIsolatedPackageExecutionContext(objectEnricher, objectPredicate, contextId.toString(), apFile, true);
        try {
            AutomationPackage automationPackage = executionContext.getAutomationPackage();
            String apName = automationPackage.getAttribute(AbstractOrganizableObject.NAME);

            // we have resolved the name of ap, and we need to save this name as custom field in resource to look up this resource during re-execution
            if (apFile.getResource() != null) {
                repository.setApNameForResource(apFile.getResource(), apName);
            }

            executions = runExecutions(automationPackage, repoId, parameters.getOriginalRepositoryObject(), contextId, executionContext.getAutomationPackageManager(), parameters, objectEnricher);
        } finally {
            // after all plans are executed we can clean up the context (remove temporary files prepared for isolated execution)
            waitForAllLaunchedExecutions(executions, apFile.getFile().getName(), executionContext);
        }
        return executions;
    }

    private List<String> runExecutions(AutomationPackage automationPackage,
                                       String repoId, RepositoryObjectReference originalRepositoryObject,
                                       ObjectId contextId, AutomationPackageManager apManager,
                                       AutomationPackageExecutionParameters parameters,
                                       ObjectEnricher objectEnricher) {
        List<String> executions = new ArrayList<>();
        List<Plan> applicablePlans = new ArrayList<>();
        PlanFilter planFilter = parameters.getPlanFilter();
        boolean somePlansFiltered = false;
        for (Plan plan : apManager.getPackagePlans(automationPackage.getId())) {
            if ((planFilter == null || planFilter.isSelected(plan)) && plan.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution()) {
                applicablePlans.add(plan);
            } else {
                somePlansFiltered = true;
            }
        }

        if (parameters.getWrapIntoTestSet() == null || !parameters.getWrapIntoTestSet()) {
            // run each plans in separate execution (apply the plan name filter to use the single file in execution)
            for (Plan plan : applicablePlans) {
                ExecutionParameters params = prepareExecutionParams(
                        parameters, automationPackage.getAttribute(AbstractOrganizableObject.NAME), contextId, repoId, originalRepositoryObject, plan.getAttribute(AbstractOrganizableObject.NAME), CommonExecutionParameters.defaultDescription(plan), objectEnricher
                );
                String newExecutionId = this.scheduler.execute(params);
                if (newExecutionId != null) {
                    executions.add(newExecutionId);
                }
            }
        } else {
            // wrap all plans in test set
            ExecutionParameters params = prepareExecutionParams(
                    parameters, automationPackage.getAttribute(AbstractOrganizableObject.NAME), contextId, repoId, originalRepositoryObject, somePlansFiltered ? applicablePlans.stream().map(p -> p.getAttribute(AbstractOrganizableObject.NAME)).collect(Collectors.joining(",")) : null, null, objectEnricher
            );
            String newExecutionId = this.scheduler.execute(params);
            if (newExecutionId != null) {
                executions.add(newExecutionId);
            }
        }
        return executions;
    }

    private ExecutionParameters prepareExecutionParams(AutomationPackageExecutionParameters parameters, String apName,
                                                       ObjectId contextId, String repoId,
                                                       RepositoryObjectReference originalRepositoryObject,
                                                       String includePlans, String defaultDescription, ObjectEnricher objectEnricher) {
        ExecutionParameters params = parameters.toExecutionParameters();

        HashMap<String, String> repositoryParameters = new HashMap<>();

        // save apName + contextId + planName to support re-execution
        repositoryParameters.put(IsolatedAutomationPackageRepository.AP_NAME, apName);
        if (contextId != null) {
            repositoryParameters.put(IsolatedAutomationPackageRepository.REPOSITORY_PARAM_CONTEXTID, contextId.toString());
        }
        if (includePlans != null) {
            repositoryParameters.put(ArtifactRepositoryConstants.PARAM_INCLUDE_PLANS, includePlans);
        }
        repositoryParameters.put(ArtifactRepositoryConstants.PARAM_WRAP_PLANS_INTO_TEST_SET, parameters.getWrapIntoTestSet() == null ? null : parameters.getWrapIntoTestSet().toString());
        repositoryParameters.put(ArtifactRepositoryConstants.PARAM_THREAD_NUMBER, parameters.getNumberOfThreads() == null ? null : parameters.getNumberOfThreads().toString());

        // store the reference from original repository object
        if (originalRepositoryObject != null && originalRepositoryObject.getRepositoryParameters() != null) {
            repositoryParameters.putAll(originalRepositoryObject.getRepositoryParameters());
        }

        params.setRepositoryObject(new RepositoryObjectReference(repoId, repositoryParameters));
        if (defaultDescription != null) {
            params.setDescription(defaultDescription);
        }

        // for instance, set the project for multitenant application
        if (objectEnricher != null) {
            objectEnricher.accept(params);
        }
        return params;
    }

    public void shutdown() throws InterruptedException {
        this.delayedCleanupExecutor.shutdown();
        boolean terminated = this.delayedCleanupExecutor.awaitTermination(60, TimeUnit.SECONDS);
        if (!terminated) {
            log.warn("Unable to terminate the execution cleanup");
        }
    }

    protected void waitForAllLaunchedExecutions(List<String> executions, String fileName, RepositoryWithAutomationPackageSupport.PackageExecutionContext executionContext) {
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
            }, isolatedExecutionTimeout, CLEANUP_POLLING_INTERVAL);
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
