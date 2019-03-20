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

import step.artefacts.Script;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.expressions.ExpressionHandler;

public class ScriptHandler extends ArtefactHandler<Script, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Script testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Script testArtefact) {
		ExpressionHandler expressionHandler = context.getExpressionHandler();
		Map<String, Object> bindings = context.getVariablesManager().getAllVariables();
		
		expressionHandler.evaluateGroovyExpression(testArtefact.getScript(), bindings);
		node.setStatus(ReportNodeStatus.PASSED);
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Script testArtefact) {
		return new ReportNode();
	}
}
