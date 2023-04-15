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
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.functions.ArtefactFunction;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.*;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalRepository extends AbstractRepository {

	private final PlanAccessor planAccessor;
	private final FunctionAccessor functionAccessor;

	public LocalRepository(PlanAccessor planAccessor, FunctionAccessor functionAccessor) {
		super(Set.of(RepositoryObjectReference.PLAN_ID, RepositoryObjectReference.FUNCTION_ID));
		this.planAccessor = planAccessor;
		this.functionAccessor = functionAccessor;
	}

	@Override
	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
		ResolvedPlan resolvedPlan = resolvePlan(repositoryParameters);

		ArtefactInfo info = new ArtefactInfo();
		if (resolvedPlan.getPlan() != null) {
			info.setName(resolvedPlan.getPlan().getAttributes() != null ? resolvedPlan.getPlan().getAttributes().get(AbstractOrganizableObject.NAME) : null);
			info.setType(AbstractArtefact.getArtefactName(resolvedPlan.getPlan().getRoot().getClass()));
		}
		return info;
	}

	@Override
	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception {
		TestSetStatusOverview testSetStatusOverview = new TestSetStatusOverview();

		ResolvedPlan resolvedPlan = resolvePlan(repositoryParameters);

		if (resolvedPlan.getPlan() != null) {
			AbstractArtefact rootArtefact = resolvedPlan.getPlan().getRoot();

			if (rootArtefact instanceof TestSet) {
				// Perform a very basic parsing of the artefact tree to get a list of test cases referenced
				// in this test set. Only direct children of the root node are considered
				List<AbstractArtefact> children = rootArtefact.getChildren();
				children.forEach(child -> {
					if (child instanceof TestCase) {
						addTestRunStatus(testSetStatusOverview.getRuns(), child);
					} else if (child instanceof CallPlan) {
						Plan referencedPlan = planAccessor.get(((CallPlan) child).getPlanId());
						AbstractArtefact root = referencedPlan.getRoot();
						if (root instanceof TestCase) {
							addTestRunStatus(testSetStatusOverview.getRuns(), root);
						}
					}
				});
			}
		}
		return testSetStatusOverview;
	}

	private ResolvedPlan resolvePlan(Map<String, String> repositoryParameters) {
		String planId = getPlanId(repositoryParameters);
		String functionId = getFunctionId(repositoryParameters);

		Plan plan = null;
		boolean embedded = false;

		if (planId != null) {
			plan = planAccessor.get(planId);
		} else if (functionId != null) {
			embedded = true;
			Function function = functionAccessor.get(functionId);
			if (function instanceof ArtefactFunction) {
				plan = ((ArtefactFunction) function).getPlan();
			}
		}
		return new ResolvedPlan(plan, embedded);
	}

	private static String getPlanId(Map<String, String> repositoryParameters) {
		return repositoryParameters.get(RepositoryObjectReference.PLAN_ID);
	}

	private static String getFunctionId(Map<String, String> repositoryParameters) {
		return repositoryParameters.get(RepositoryObjectReference.FUNCTION_ID);
	}

	private void addTestRunStatus(List<TestRunStatus> testRunStatusList, AbstractArtefact abstractArtefact) {
		testRunStatusList.add(new TestRunStatus(abstractArtefact.getId().toString(),
				abstractArtefact.getAttributes().get(AbstractOrganizableObject.NAME), ReportNodeStatus.NORUN));
	}

	@Override
	public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters)
			throws Exception {
		ImportResult importResult = new ImportResult();
		ResolvedPlan resolvedPlan = resolvePlan(repositoryParameters);

		if (resolvedPlan.getPlan() != null) {
			if (!resolvedPlan.isEmbeddedInFunction()) {
				importResult.setPlanId(resolvedPlan.getPlan().getId().toString());
				importResult.setPlan(resolvedPlan.getPlan());

				// TODO: context plan accessor for composite function?...
				PlanAccessor contextPlanAccessor = context.getPlanAccessor();
				if (contextPlanAccessor.get(resolvedPlan.getPlan().getId().toString()) == null) {
					contextPlanAccessor.save(resolvedPlan.getPlan());
				}
			} else {
				importResult.setPlan(resolvedPlan.getPlan());
			}

			importResult.setSuccessful(true);
		}
		return importResult;
	}

	@Override
	public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
		// The local repository doesn't perform any export
	}

	private static class ResolvedPlan {
		private final Plan plan;
		private boolean embeddedInFunction = false;

		public ResolvedPlan(Plan plan, boolean embeddedInFunction) {
			this.plan = plan;
			this.embeddedInFunction = embeddedInFunction;
		}

		public Plan getPlan() {
			return plan;
		}

		public boolean isEmbeddedInFunction() {
			return embeddedInFunction;
		}
	}
}
