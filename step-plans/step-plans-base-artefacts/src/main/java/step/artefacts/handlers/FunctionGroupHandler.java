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
package step.artefacts.handlers;

import step.artefacts.FunctionGroup;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.artefacts.handlers.functions.FunctionGroupSession;
import step.artefacts.handlers.functions.TokenForecastingContext;
import step.core.AbstractContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.functions.FunctionGroupHandle;
import step.functions.execution.FunctionExecutionService;
import step.grid.tokenpool.Interest;

import java.util.Map;
import java.util.function.BiConsumer;

public class FunctionGroupHandler extends ArtefactHandler<FunctionGroup, ReportNode> implements FunctionGroupHandle {
	
	public static final String FUNCTION_GROUP_CONTEXT_KEY = "##functionGroupContext##";

	private TokenSelectorHelper tokenSelectorHelper;
	private FunctionExecutionService functionExecutionService;

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
		TokenForecastingContext tokenForecastingContext = TokenForecastingExecutionPlugin.getTokenForecastingContext(context);
		// Inject the mocked function execution service of the token forecasting context instead of the function execution service of the context
		FunctionExecutionService functionExecutionService = tokenForecastingContext.getFunctionExecutionServiceForTokenForecasting();
		FunctionGroupContext handle = buildFunctionGroupContext(functionExecutionService, testArtefact);
		try {
			addFunctionGroupContextToContext(node, handle);
			BiConsumer<AbstractArtefact, ReportNode> consumer = testArtefact.getConsumer();
			if(consumer == null) {
				SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
				scheduler.createReportSkeleton_(node, testArtefact);
			} else {
				consumer.accept(testArtefact, node);
			}
		} finally {
			try {
				handle.session.close();
			} catch (Exception e) {
				// TODO
			}
		}
	}

	public static class FunctionGroupContext {

		private final FunctionGroupSession session;

		private final Map<String, Interest> functionGroupTokenSelectionCriteria;

		public FunctionGroupContext(FunctionExecutionService functionExecutionService, Map<String, Interest> functionGroupTokenSelectionCriteria) {
			super();
			this.functionGroupTokenSelectionCriteria = functionGroupTokenSelectionCriteria;
			this.session = new FunctionGroupSession(functionExecutionService);
		}

		public Map<String, Interest> getFunctionGroupTokenSelectionCriteria() {
			return functionGroupTokenSelectionCriteria;
		}

		public FunctionGroupSession getSession() {
			return session;
		}
	}
	
	@Override
	protected void execute_(ReportNode node, FunctionGroup functionGroup) throws Exception {
		FunctionGroupContext functionGroupContext = buildFunctionGroupContext(functionExecutionService, functionGroup);
		addFunctionGroupContextToContext(node, functionGroupContext);
		try {
			BiConsumer<AbstractArtefact, ReportNode> consumer = functionGroup.getConsumer();
			if(consumer == null) {
				SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
				scheduler.execute_(node, functionGroup);
			} else {
				consumer.accept(functionGroup, node);
			}
		} finally {
			releaseTokens(context, true);
		}	
	}

	private void addFunctionGroupContextToContext(ReportNode node, FunctionGroupContext functionGroupContext) {
		context.getVariablesManager().putVariable(node, FUNCTION_GROUP_CONTEXT_KEY, functionGroupContext);
		context.put(FunctionGroupHandle.class, this);
	}

	private FunctionGroupContext buildFunctionGroupContext(FunctionExecutionService functionExecutionService, FunctionGroup functionGroup) {
		Map<String, Interest> additionalSelectionCriteria = tokenSelectorHelper.getTokenSelectionCriteria(functionGroup, getBindings());
		return new FunctionGroupContext(functionExecutionService, additionalSelectionCriteria);
	}

	@Override
	public void releaseTokens(AbstractContext context, boolean local) throws Exception {
		FunctionGroupContext handle = (FunctionGroupContext) ((ExecutionContext) context).getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
		handle.session.releaseTokens(local);
	}
	
	@Override
	public boolean isInSession(AbstractContext context) {
		return ((ExecutionContext) context).getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY) != null;
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, FunctionGroup testArtefact) {
		return new ReportNode();
	}
}
