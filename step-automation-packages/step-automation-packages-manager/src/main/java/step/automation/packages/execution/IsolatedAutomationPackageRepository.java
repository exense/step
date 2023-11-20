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

import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.repositories.*;
import step.functions.accessor.FunctionAccessor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IsolatedAutomationPackageRepository extends AbstractRepository {

    public static final String REPOSITORY_PARAM_CONTEXTID = "contextid";

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
        // TODO: type?
        info.setType("automationPackage");
        info.setName(automationPackage.getAttribute(AbstractOrganizableObject.NAME));
        return info;
    }

    private AutomationPackage getAutomationPackageForContext(Map<String, String> repositoryParameters) {
        // we expect, that there is only one automation package stored per context
        AutomationPackageManager automationPackageManager = getContext(repositoryParameters);
        return automationPackageManager.getAllAutomationPackages().findFirst().orElse(null);
    }

    private AutomationPackageManager getContext(Map<String, String> repositoryParameters) {
        return inMemoryPackageManagers.get(repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID));
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) {
        AutomationPackageManager automationPackageManager = getContext(repositoryParameters);
        AutomationPackage automationPackage = getAutomationPackageForContext(repositoryParameters);

        String planId = repositoryParameters.get(RepositoryObjectReference.PLAN_ID);

        Plan plan = automationPackageManager.getPackagePlans(automationPackage.getId().toString())
                .stream()
                .filter(p -> p.getId().toString().equals(planId)).findFirst().orElse(null);
        if (plan == null) {
            return null;
        }

        enrichPlan(context, plan);
        context.getPlanAccessor().save(plan);

        ImportResult result = new ImportResult();
        result.setPlanId(plan.getId().toString());

        plan.getFunctions().iterator().forEachRemaining(f -> (context.get(FunctionAccessor.class)).save(f));
        automationPackageManager.getPackageFunctions(automationPackage.getId().toString()).forEach(f -> context.get(FunctionAccessor.class).save(f));

        result.setSuccessful(true);

        return result;
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception {
        // TODO: ...
        return null;
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
        // TODO: ...
    }

    public void clearContext(String contextId){
        // TODO: cleanup resources if needed
        this.inMemoryPackageManagers.remove(contextId);
    }

    public void putContext(String contextId, AutomationPackageManager automationPackageManager) {
        if (this.inMemoryPackageManagers.get(contextId) != null) {
            throw new IllegalArgumentException("Context " + contextId + " already exists");
        }
        this.inMemoryPackageManagers.put(contextId, automationPackageManager);
    }
}
