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
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageManagerException;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.TestSetStatusOverview;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.repositories.ArtifactRepositoryConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The repository for artifacts already stored (deployed) in Step DB as automation packages
 */
public class LocalAutomationPackageRepository extends RepositoryWithAutomationPackageSupport {

    public LocalAutomationPackageRepository(AutomationPackageManager manager, FunctionTypeRegistry functionTypeRegistry, FunctionAccessor functionAccessor) {
        super(Set.of(REPOSITORY_PARAM_CONTEXTID), manager, functionTypeRegistry, functionAccessor);
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
        String apName = repositoryParameters.get(AP_NAME);

        ArtefactInfo info = new ArtefactInfo();
        info.setType("automationPackage");
        info.setName(apName);
        return info;
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) throws Exception {
        return new TestSetStatusOverview();
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {

    }

    @Override
    protected PackageExecutionContext getOrRestorePackageExecutionContext(Map<String, String> repositoryParameters, ObjectEnricher enricher, ObjectPredicate predicate) {
        String contextId = repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID);

        // Execution context can be created in-advance and shared between several plans
        PackageExecutionContext current = contextId == null ? null : sharedPackageExecutionContexts.get(contextId);
        if (current == null) {
            if (contextId == null) {
                contextId = new ObjectId().toString();
            }

            String apName = repositoryParameters.get(AP_NAME);
            if (apName == null) {
                throw new AutomationPackageManagerException("Unable to resolve automation package name for local execution");
            }
            AutomationPackage automationPackage = manager.getAutomationPackageByName(apName, predicate);
            if (automationPackage == null) {
                throw new AutomationPackageManagerException("Unable to resolve automation package by name: " + apName);
            }

            return new LocalPackageExecutionContext(contextId, manager, automationPackage);
        }
        return current;
    }

    @Override
    protected ImportResult importPlanForExecutionWithinAp(ExecutionContext context,
                                                          ImportResult result, Plan plan,
                                                          AutomationPackageManager apManager,
                                                          AutomationPackage automationPackage,
                                                          boolean fakeWrappedPlan) {
        // if the plan is wrapped, we have to store it (temporarily) in plan accessor
        if (fakeWrappedPlan) {
            enrichPlan(context, plan);

            // the plan accessor in context should be layered with 'inMemory' accessor on the top to temporarily store
            // all plans from AP (in code below)
            PlanAccessor planAccessor = context.getPlanAccessor();
            if (!isLayeredAccessor(planAccessor)) {
                result.setErrors(List.of(planAccessor.getClass() + " is not layered"));
                return result;
            }
            planAccessor.save(plan);
        }
        ImportResult importResult = new ImportResult();
        importResult.setSuccessful(true);
        importResult.setPlanId(plan.getId().toString());
        return importResult;
    }

    protected boolean isWrapPlansIntoTestSet(Map<String, String> repositoryParameters) {
        return Boolean.parseBoolean(repositoryParameters.getOrDefault(ArtifactRepositoryConstants.PARAM_WRAP_PLANS_INTO_TEST_SET, "true"));
    }
}
