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

import step.artefacts.FunctionGroup;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionTokenHandle;
import step.plugins.adaptergrid.GridPlugin;

public class FunctionGroupHandler extends ArtefactHandler<FunctionGroup, ReportNode> {
	
	public static final String TOKEN_PARAM_KEY = "##token##";

	private FunctionClient functionClient;
	private TokenSelectorHelper tokenSelectorHelper;
	
	public FunctionGroupHandler() {
		super();
		functionClient = (FunctionClient) context.getGlobalContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
		tokenSelectorHelper = new TokenSelectorHelper(functionClient,  new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getGlobalContext().getExpressionHandler())));
	}

	@Override
	protected void createReportSkeleton_(ReportNode node, FunctionGroup testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.createReportSkeleton_(node, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, FunctionGroup testArtefact) {		
		FunctionTokenHandle token = tokenSelectorHelper.selectToken(testArtefact, functionClient, getBindings());
		context.getVariablesManager().putVariable(node, TOKEN_PARAM_KEY, token);
		try {
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
			scheduler.execute_(node, testArtefact);
		} finally {
			token.release();
		}	
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, FunctionGroup testArtefact) {
		return new ReportNode();
	}
}
