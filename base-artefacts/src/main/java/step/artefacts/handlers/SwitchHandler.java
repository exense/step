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

import step.artefacts.Case;
import step.artefacts.Switch;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class SwitchHandler extends ArtefactHandler<Switch, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode,
			Switch testArtefact) {
		delegate(parentNode, testArtefact, false);
	}

	@Override
	protected void execute_(ReportNode node, Switch testArtefact) {
		delegate(node, testArtefact, true);
	}
	
	private void delegate(ReportNode node, Switch testArtefact, boolean execution) {
		String evaluationResultStr = testArtefact.getExpression().get();
		
		node.setStatus(ReportNodeStatus.PASSED);
						
		for(AbstractArtefact child:getChildren(testArtefact)) {
			if(child instanceof Case) {
				Case c = (Case) child;
				context.getGlobalContext().getDynamicBeanResolver().evaluate(c, context.getVariablesManager().getAllVariables());
				
				if(evaluationResultStr.equals(c.getValue().get())) {
					if(execution) {
						ReportNode result = delegateExecute(c, node);
						node.setStatus(result.getStatus());
					} else {
						delegateCreateReportSkeleton(c, node);
					}
					break;
				} 
			}
		}
			
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode,
			Switch testArtefact) {
		return new ReportNode();
	}

}
