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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.*;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.resources.LayeredResourceAccessor;
import step.resources.LayeredResourceManager;
import step.resources.ResourceAccessor;
import step.resources.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IsolatedAutomationPackageRepository extends AbstractRepository {

    public static final String REPOSITORY_PARAM_CONTEXTID = "contextid";

    public static final Logger log = LoggerFactory.getLogger(IsolatedAutomationPackageRepository.class);

    private final ConcurrentHashMap<String, AutomationPackageManager> inMemoryPackageManagers = new ConcurrentHashMap<>();

    protected IsolatedAutomationPackageRepository() {
        super(Set.of(REPOSITORY_PARAM_CONTEXTID));
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
        AutomationPackage automationPackage = getAutomationPackageForContext(repositoryParameters);
        if (automationPackage == null) {
            return null;
        }
        ArtefactInfo info = new ArtefactInfo();
        info.setType("automationPackage");
        info.setName(automationPackage.getAttribute(AbstractOrganizableObject.NAME));
        return info;
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) throws Exception {
        return new TestSetStatusOverview();
    }

    private AutomationPackage getAutomationPackageForContext(Map<String, String> repositoryParameters) {
        // we expect, that there is only one automation package stored per context
        AutomationPackageManager automationPackageManager = getContext(repositoryParameters);
        return automationPackageManager.getAllAutomationPackages(null).findFirst().orElse(null);
    }

    private AutomationPackageManager getContext(Map<String, String> repositoryParameters) {
        return inMemoryPackageManagers.get(repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID));
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) {
        AutomationPackageManager automationPackageManager = getContext(repositoryParameters);
        AutomationPackage automationPackage = getAutomationPackageForContext(repositoryParameters);

        String planId = repositoryParameters.get(RepositoryObjectReference.PLAN_ID);
        ImportResult result = new ImportResult();
        result.setPlanId(planId);

        Plan plan = automationPackageManager.getPackagePlans(automationPackage.getId())
                .stream()
                .filter(p -> p.getId().toString().equals(planId)).findFirst().orElse(null);
        if (plan == null) {
            // failed result
            result.setErrors(List.of("Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " has no plan with id=" + planId));
            return result;
        }
        enrichPlan(context, plan);

        PlanAccessor planAccessor = context.getPlanAccessor();
        if (!isLayeredAccessor(planAccessor)) {
            result.setErrors(List.of(planAccessor.getClass() + " is not layered"));
            return result;
        }

        planAccessor.save(plan);

        FunctionAccessor functionAccessor = context.get(FunctionAccessor.class);
        List<Function> functionsForSave = new ArrayList<>();
        if (plan.getFunctions() != null) {
            plan.getFunctions().iterator().forEachRemaining(functionsForSave::add);
        }
        functionsForSave.addAll(automationPackageManager.getPackageFunctions(automationPackage.getId()));
        functionAccessor.save(functionsForSave);

        ResourceManager contextResourceManager = context.getResourceManager();
        if (!(contextResourceManager instanceof LayeredResourceManager)) {
            result.setErrors(List.of(contextResourceManager.getClass() + " is not layered"));
            return result;
        }

        // import all resources from automation package to execution context by adding the layer to contextResourceManager
        // resource manager used in isolated package manager is non-permanent
        ((LayeredResourceManager) contextResourceManager).pushManager(automationPackageManager.getResourceManager(), false);

        // push the resource accessor from resource manager to keep consistency between ResourceManager and ResourceAccessor
        if (automationPackageManager.getResourceManager().getResourceAccessor() != null) {
            ResourceAccessor contextResourceAccessor = context.getResourceAccessor();
            if (!isLayeredAccessor(contextResourceAccessor)) {
                result.setErrors(List.of(contextResourceAccessor.getClass() + " is not layered"));
                return result;
            }
            ((LayeredResourceAccessor) contextResourceAccessor).pushAccessor(automationPackageManager.getResourceManager().getResourceAccessor());
        }

        result.setSuccessful(true);

        return result;
    }

    private static boolean isLayeredAccessor(Accessor<?> accessor) {
        return accessor instanceof LayeredAccessor;
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {

    }

    public void cleanupContext(String contextId) {
        // only after isolated execution is finished we can clean up temporary created resources
        try {
            AutomationPackageManager automationPackageManager = this.inMemoryPackageManagers.get(contextId);
            if (automationPackageManager != null) {
                automationPackageManager.cleanup();
            }
        } finally {
            this.inMemoryPackageManagers.remove(contextId);
        }
    }

    public void putContext(String contextId, AutomationPackageManager automationPackageManager) {
        if (this.inMemoryPackageManagers.get(contextId) != null) {
            throw new IllegalArgumentException("Context " + contextId + " already exists");
        }
        this.inMemoryPackageManagers.put(contextId, automationPackageManager);
    }
}
