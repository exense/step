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
package step.repositories.staging;

import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.repositories.AbstractRepository;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.TestSetStatusOverview;
import step.functions.accessor.FunctionAccessor;

import java.util.Map;
import java.util.Set;

/**
 * @deprecated The staging client is deprecated and will be removed in future
 *             releases. All executions are now providing an isolated context
 *             thus making the Staging repository useless.
 *
 */
public class StagingRepository extends AbstractRepository {

	public static final String REPOSITORY_PARAM_CONTEXTID = "contextid";
	protected StagingContextAccessorImpl stagingContextAccessor;
	
	public StagingRepository(StagingContextAccessorImpl stagingContextRegistry) {
		super(Set.of(REPOSITORY_PARAM_CONTEXTID));
		this.stagingContextAccessor = stagingContextRegistry;
	}

	@Override
	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
		StagingContext stagingContext = stagingContextAccessor.get(repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID));
		ArtefactInfo info = new ArtefactInfo();
		info.setType("testplan");
		info.setName(stagingContext.getPlan().getRoot().getName());
		return info;
	}

	@Override
	public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
		StagingContext stagingContext = stagingContextAccessor.get(repositoryParameters.get("contextid"));

		Plan plan = stagingContext.plan;
		enrichPlan(context, plan);
		context.getPlanAccessor().save(plan);
		
		ImportResult result = new ImportResult();
		result.setPlanId(plan.getId().toString());
		
		plan.getFunctions().iterator().forEachRemaining(f->(context.get(FunctionAccessor.class)).save(f));
		
		result.setSuccessful(true);
	
		return result;
	}

	@Override
	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
