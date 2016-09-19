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

import step.artefacts.IfBlock;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.artefacts.reports.IfBlockReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.miscellaneous.TestArtefactResultHandler;
import step.expressions.ExpressionHandler;

public class IfBlockHandler extends ArtefactHandler<IfBlock, IfBlockReportNode> {

	@Override
	protected void createReportSkeleton_(IfBlockReportNode parentNode, IfBlock testArtefact) {
		evaluateExpressionAndDelegate(parentNode, testArtefact, false);
	}

	@Override
	protected void execute_(IfBlockReportNode node, IfBlock testArtefact) {
		evaluateExpressionAndDelegate(node, testArtefact, true);
	}

	private void evaluateExpressionAndDelegate(IfBlockReportNode node, IfBlock testArtefact, boolean execution) {
		ExpressionHandler expressionHandler = new ExpressionHandler();
		Map<String, Object> bindings = context.getVariablesManager().getAllVariables();
		try {
			Object checkResult = expressionHandler.evaluateGroovyExpression(testArtefact.getCondition(), bindings);
			
			if(checkResult!=null && checkResult instanceof Boolean) {
				if((boolean)checkResult) {
					SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
					if(execution) {
						scheduler.execute_(node, testArtefact);
					} else {
						scheduler.createReportSkeleton_(node, testArtefact);
					}
				} else {
					node.setStatus(ReportNodeStatus.PASSED);	
				} 
			} else {
				throw new Exception("The expression '"+testArtefact.getCondition()+"' returned '"+checkResult +"' instead of a boolean");
			}
		} catch (Exception e) {
			TestArtefactResultHandler.failWithException(node, e);
		}
	}

	@Override
	public IfBlockReportNode createReportNode_(ReportNode parentNode, IfBlock testArtefact) {
		return new IfBlockReportNode();
	}

}
