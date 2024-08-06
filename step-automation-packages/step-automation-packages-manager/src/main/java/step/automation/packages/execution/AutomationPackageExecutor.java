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

import ch.exense.commons.io.FileHelper;
import ch.exense.commons.io.Poller;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageManagerException;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.model.*;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;
import step.core.plans.filters.PlanByIncludedNamesFilter;
import step.core.repositories.RepositoryObjectReference;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;

import java.io.*;
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
    private final FunctionTypeRegistry functionTypeRegistry;
    private final FunctionAccessor functionAccessor;
    private final IsolatedAutomationPackageRepository isolatedAutomationPackageRepository;
    private final AutomationPackageManager automationPackageManager;

    public AutomationPackageExecutor(ExecutionLauncher scheduler,
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

    public String rerunPlan(ExecutionParameters rerunParameters, ObjectEnricher objectEnricher, ObjectPredicate objectPredicate) {
        if (rerunParameters.getRepositoryObject() == null) {
            throw new AutomationPackageManagerException("Repository object is not defined");
        }
        String oldContextId = rerunParameters.getRepositoryObject().getRepositoryParameters().get(IsolatedAutomationPackageRepository.REPOSITORY_PARAM_CONTEXTID);
        if (oldContextId == null) {
            throw new AutomationPackageManagerException(IsolatedAutomationPackageRepository.REPOSITORY_PARAM_CONTEXTID + " is undefined");
        }
        String oldPlanName = rerunParameters.getRepositoryObject().getRepositoryParameters().get(RepositoryObjectReference.PLAN_NAME);
        if (oldPlanName == null) {
            throw new AutomationPackageManagerException(RepositoryObjectReference.PLAN_NAME + " is undefined");
        }
        File file = isolatedAutomationPackageRepository.getFile(oldContextId);
        if (file == null) {
            throw new AutomationPackageManagerException("AP file is not stored for execution context " + oldContextId);
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            AutomationPackageExecutionParameters apExecutionParameters = new AutomationPackageExecutionParameters(
                    rerunParameters.getCustomParameters(),
                    rerunParameters.getUserID(),
                    rerunParameters.getArtefactFilter(),
                    new PlanByIncludedNamesFilter(List.of(oldPlanName)),
                    rerunParameters.getMode()
            );
            List<String> executions = runInIsolation(new ObjectId(oldContextId), file.getName(), fis, apExecutionParameters, objectEnricher, objectPredicate);
            if (executions.isEmpty()) {
                throw new AutomationPackageManagerException("Cannot obtain execution result for plan " + oldPlanName);
            }
            return executions.get(0);
        } catch (IOException e) {
            throw new AutomationPackageManagerException("Cannot execute automation package", e);
        }
    }

    // TODO: temp solution
    private File copyStreamToTempFile(InputStream in, String fileName) throws IOException {
        // create temp folder to keep the original file name
        File newFolder = FileHelper.createTempFolder();
        newFolder.deleteOnExit();
        File newFile = new File(newFolder, fileName);
        newFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(newFile)) {
            IOUtils.copy(in, out);
        }
        return newFile;
    }

    public List<String> runInIsolation(InputStream automationPackage, String fileName, AutomationPackageExecutionParameters parameters,
                                       ObjectEnricher objectEnricher, ObjectPredicate objectPredicate) {
        ObjectId contextId = new ObjectId();


        // store file in temporary storage to support rerun
        // TODO: rewrite files for the same automation package (don't store separate file per context)
        File file = null;
        try {
            file = copyStreamToTempFile(automationPackage, fileName);
            isolatedAutomationPackageRepository.putFile(contextId.toString(), file);
        } catch (IOException ex) {
            throw new AutomationPackageManagerException("Cannot execute automation package " + fileName, ex);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return runInIsolation(contextId, fileName, fis, parameters, objectEnricher, objectPredicate);
        } catch (IOException ex) {
            throw new AutomationPackageManagerException("Cannot execute automation package " + fileName, ex);
        }
    }

    private List<String> runInIsolation(ObjectId contextId, String fileName, FileInputStream fis, AutomationPackageExecutionParameters parameters,
                                        ObjectEnricher objectEnricher, ObjectPredicate objectPredicate) {
        List<String> executions = new ArrayList<>();
        try {
            // prepare the isolated in-memory automation package manager with the only one automation package
            AutomationPackageManager inMemoryPackageManager = automationPackageManager.createIsolated(
                    contextId, functionTypeRegistry,
                    functionAccessor
            );

            ObjectId packageId = inMemoryPackageManager.createAutomationPackage(fis, fileName, objectEnricher, objectPredicate);

            isolatedAutomationPackageRepository.putContext(contextId.toString(), inMemoryPackageManager);

            for (Plan plan : inMemoryPackageManager.getPackagePlans(packageId)) {
                PlanFilter planFilter = parameters.getPlanFilter();

                if ((planFilter == null || planFilter.isSelected(plan)) && plan.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution()) {
                    ExecutionParameters params = parameters.toExecutionParameters();
                    params.setIsolatedExecution(true);
                    HashMap<String, String> repositoryParameters = new HashMap<>();
                    repositoryParameters.put(IsolatedAutomationPackageRepository.REPOSITORY_PARAM_CONTEXTID, contextId.toString());
                    repositoryParameters.put(RepositoryObjectReference.PLAN_ID, plan.getId().toString());

                    // store plan name to support re-run for this plan (plan_id is not enough for in-memory executions)
                    repositoryParameters.put(RepositoryObjectReference.PLAN_NAME, plan.getAttribute(AbstractOrganizableObject.NAME));

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
