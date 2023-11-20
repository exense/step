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

import org.bson.types.ObjectId;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.accessor.InMemoryAutomationPackageAccessorImpl;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
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

public class AutomationPackageExecutor {

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
        String packageId = inMemoryPackageManager.createAutomationPackage(automationPackage, fileName, objectEnricher);

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
            isolatedAutomationPackageRepository.clearContext(contextId.toString());
        }
        return executions;
    }

    private AutomationPackageManager createInMemoryAutomationPackageManager(ObjectId contextId) {
        // TODO: extended local resource manager
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
