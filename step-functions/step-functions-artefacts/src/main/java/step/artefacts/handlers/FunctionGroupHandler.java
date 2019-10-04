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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import step.artefacts.FunctionGroup;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
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
		
		final List<TokenWrapper> tokens = new ArrayList<>();
		
		TokenWrapper localToken;
		
		final Map<String, Interest> additionalSelectionCriteria;

		public FunctionGroupContext(Map<String, Interest> additionalSelectionCriteria) {
			super();
			this.additionalSelectionCriteria = additionalSelectionCriteria;
		}
		
		public List<TokenWrapper> getTokens() {
			return tokens;
		}

		public boolean addToken(TokenWrapper e) {
			return tokens.add(e);
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
			List<Exception> releaseExceptions = new ArrayList<>();
			if(handle.getTokens()!=null) {
				handle.getTokens().forEach(t->{
					try {
						functionExecutionService.returnTokenHandle(t.getID());
					} catch (FunctionExecutionServiceException e) {
						releaseExceptions.add(e);
					}
				});
			}
			if(handle.getLocalToken()!=null) {
				try {
					functionExecutionService.returnTokenHandle(handle.getLocalToken().getID());
				} catch (FunctionExecutionServiceException e) {
					releaseExceptions.add(e);
				}
			}
			
			int exceptionCount = releaseExceptions.size();
			if(exceptionCount > 0) {
				if(exceptionCount == 1) {
					throw releaseExceptions.get(0);
				} else {
					throw new Exception("Multiple errors occurred when releasing agent tokens: "+
								releaseExceptions.stream().map(e->e.getMessage()).collect(Collectors.joining(", ")));
				}
			}
		}	
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, FunctionGroup testArtefact) {
		return new ReportNode();
	}
}
