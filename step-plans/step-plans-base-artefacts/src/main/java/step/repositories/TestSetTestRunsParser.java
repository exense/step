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

import step.artefacts.CallPlan;
import step.artefacts.TestCase;
import step.artefacts.TestSet;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.TestRunStatus;
import step.expressions.ExpressionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs a very basic parsing of the artefact tree of a plan to get the list of test cases referenced
 * by its {@link TestSet} root. Only direct children of the root node are considered: {@link TestCase}s and
 * {@link CallPlan}s whose referenced plan has a {@link TestCase} root.
 */
public class TestSetTestRunsParser {

    private final PlanLocator planLocator;

    public TestSetTestRunsParser(PlanAccessor planAccessor, ExpressionHandler expressionHandler) {
        DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(expressionHandler));
        planLocator = new PlanLocator(planAccessor, new SelectorHelper(dynamicJsonObjectResolver));
    }

    public static boolean isTestSet(Plan plan) {
        return plan != null && plan.getRoot() instanceof TestSet;
    }

    public List<TestRunStatus> getTestRuns(Plan plan, ObjectPredicate objectPredicate) {
        List<TestRunStatus> testRuns = new ArrayList<>();
        if (isTestSet(plan)) {
            plan.getRoot().getChildren().forEach(child -> {
                if (child instanceof TestCase) {
                    testRuns.add(toTestRunStatus(child));
                } else if (child instanceof CallPlan) {
                    Plan referencedPlan = planLocator.selectPlan((CallPlan) child, objectPredicate, null);
                    if (referencedPlan != null && referencedPlan.getRoot() instanceof TestCase) {
                        testRuns.add(toTestRunStatus(referencedPlan.getRoot()));
                    }
                }
            });
        }
        return testRuns;
    }

    private static TestRunStatus toTestRunStatus(AbstractArtefact artefact) {
        return new TestRunStatus(artefact.getId().toString(), artefact.getAttributes().get(AbstractOrganizableObject.NAME), ReportNodeStatus.NORUN);
    }
}
