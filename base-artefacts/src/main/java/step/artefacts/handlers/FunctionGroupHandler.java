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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.FunctionGroup;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionTokenHandle;
import step.grid.tokenpool.Interest;
import step.plugins.adaptergrid.GridPlugin;

public class FunctionGroupHandler extends ArtefactHandler<FunctionGroup, ReportNode> {

	private static final Logger logger = LoggerFactory.getLogger(FunctionGroupHandler.class);
	
	public static final String TOKEN_PARAM_KEY = "##token##";

	@Override
	protected void createReportSkeleton_(ReportNode node, FunctionGroup testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.createReportSkeleton_(node, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, FunctionGroup testArtefact) {
		FunctionClient functionClient = (FunctionClient) ExecutionContext.getCurrentContext().getGlobalContext().get(GridPlugin.FUNCTIONCLIENT_KEY);

		Map<String, Interest> interests = new HashMap<>();
		if(testArtefact.getSelectionCriteria()!=null) {
			testArtefact.getSelectionCriteria().forEach((e,v)->interests.put(e, new Interest(Pattern.compile(v), true)));
		}
		FunctionTokenHandle token = functionClient.getFunctionToken(testArtefact.getAttributes(), interests);
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
