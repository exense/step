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
package step.repositories;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.*;
import step.expressions.ExpressionHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalRepository extends AbstractRepository {

    private final PlanAccessor planAccessor;
    private final TestSetTestRunsParser testSetTestRunsParser;

    public LocalRepository(PlanAccessor planAccessor, ExpressionHandler expressionHandler) {
        super(Set.of(RepositoryObjectReference.PLAN_ID));
        this.planAccessor = planAccessor;
        testSetTestRunsParser = new TestSetTestRunsParser(planAccessor, expressionHandler);
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
        String planId = getPlanId(repositoryParameters);
        Plan plan = planAccessor.get(planId);

        ArtefactInfo info = new ArtefactInfo();
        info.setName(plan.getAttributes() != null ? plan.getAttributes().get(AbstractOrganizableObject.NAME) : null);
        info.setType(AbstractArtefact.getArtefactName(plan.getRoot().getClass()));
        return info;
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate, String actorUser) throws Exception {
        TestSetStatusOverview testSetStatusOverview = new TestSetStatusOverview();

        String planId = getPlanId(repositoryParameters);
        Plan plan = planAccessor.get(planId);

        testSetStatusOverview.setRuns(testSetTestRunsParser.getTestRuns(plan, objectPredicate));
        return testSetStatusOverview;
    }

    public static String getPlanId(Map<String, String> repositoryParameters) {
        return repositoryParameters.get(RepositoryObjectReference.PLAN_ID);
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters)
        throws Exception {
        ImportResult importResult = new ImportResult();
        String planId = getPlanId(context.getExecutionParameters().getRepositoryObject().getRepositoryParameters());
        Plan plan = context.getPlanAccessor().get(planId);
        if (plan == null) {
            importResult.setErrors(List.of("The plan with id '" + planId + "' does not exist. It may have been deleted."));
            importResult.setSuccessful(false);
        } else {
            importResult.setPlanId(planId);
            importResult.setSuccessful(true);
        }
        return importResult;
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
        // The local repository doesn't perform any export
    }
}
