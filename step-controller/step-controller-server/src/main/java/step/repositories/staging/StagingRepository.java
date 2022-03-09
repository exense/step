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

import java.util.Map;

import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.repositories.*;
import step.functions.accessor.FunctionAccessor;

/**
 * @deprecated The staging client is deprecated and will be removed in future
 *             releases. All executions are now providing an isolated context
 *             thus making the Staging repository useless.
 *
 */
public class StagingRepository extends AbstractRepository {

	protected StagingContextAccessorImpl stagingContextAccessor;
	
	public StagingRepository(StagingContextAccessorImpl stagingContextRegistry) {
		super();
		this.stagingContextAccessor = stagingContextRegistry;
	}

	@Override
	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
		StagingContext stagingContext = stagingContextAccessor.get(repositoryParameters.get("contextid"));
		ArtefactInfo info = new ArtefactInfo();
		info.setType("testplan");
		info.setName(stagingContext.getPlan().getRoot().getAttributes().get("name"));
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
