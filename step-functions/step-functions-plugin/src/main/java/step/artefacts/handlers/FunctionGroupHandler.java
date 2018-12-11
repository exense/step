/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers;

import java.util.Map;

import step.artefacts.FunctionGroup;
import step.artefacts.handlers.SequentialArtefactScheduler;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.functions.execution.FunctionExecutionService;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;

public class FunctionGroupHandler extends ArtefactHandler<FunctionGroup, ReportNode> {
	
	public static final String FUNCTION_GROUP_CONTEXT_KEY = "##functionGroupContext##";

	private FunctionExecutionService functionExecutionService;
	private TokenSelectorHelper tokenSelectorHelper;
	
	public FunctionGroupHandler() {
		super();
	}

	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		functionExecutionService = context.get(FunctionExecutionService.class);
		tokenSelectorHelper = new TokenSelectorHelper(new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler())));

	}

	@Override
	protected void createReportSkeleton_(ReportNode node, FunctionGroup testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
		scheduler.createReportSkeleton_(node, testArtefact);
	}

	public static class FunctionGroupContext {
		
		TokenWrapper token;
		
		TokenWrapper localToken;
		
		final Map<String, Interest> additionalSelectionCriteria;

		public FunctionGroupContext(Map<String, Interest> additionalSelectionCriteria) {
			super();
			this.additionalSelectionCriteria = additionalSelectionCriteria;
		}

		public TokenWrapper getToken() {
			return token;
		}

		public void setToken(TokenWrapper token) {
			this.token = token;
		}

		public TokenWrapper getLocalToken() {
			return localToken;
		}

		public void setLocalToken(TokenWrapper localToken) {
			this.localToken = localToken;
		}

		public Map<String, Interest> getAdditionalSelectionCriteria() {
			return additionalSelectionCriteria;
		}
		
	}
	
	@Override
	protected void execute_(ReportNode node, FunctionGroup testArtefact) throws Exception {		
		Map<String, Interest> additionalSelectionCriteria = tokenSelectorHelper.getTokenSelectionCriteria(testArtefact, getBindings());
		FunctionGroupContext handle = new FunctionGroupContext(additionalSelectionCriteria);
		context.getVariablesManager().putVariable(node, FUNCTION_GROUP_CONTEXT_KEY, handle);
		try {
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
			scheduler.execute_(node, testArtefact);
		} finally {
			if(handle.getToken()!=null) {
				functionExecutionService.returnTokenHandle(handle.getToken());
			}
		}	
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, FunctionGroup testArtefact) {
		return new ReportNode();
	}
}
