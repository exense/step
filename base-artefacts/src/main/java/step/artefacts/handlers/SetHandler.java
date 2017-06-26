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

import step.artefacts.Set;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.variables.ImmutableVariableException;
import step.core.variables.UndefinedVariableException;
import step.core.variables.VariablesManager;

public class SetHandler extends ArtefactHandler<Set, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Set testArtefact) {
		executeSet(parentNode, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, Set testArtefact) {
		node.setStatus(ReportNodeStatus.PASSED);
		executeSet(node, testArtefact);
	}

	private void executeSet(ReportNode node, Set testArtefact) {
		if(testArtefact.getKey()!=null) {
			Object result;			
			if(testArtefact.getValue()!=null) {
				result= testArtefact.getValue().get();				
			} else {
				result = null;
			}				
			
			VariablesManager varMan = context.getVariablesManager();
			try {
				varMan.updateVariable(testArtefact.getKey().get(), result);
			} catch(UndefinedVariableException|ImmutableVariableException e) {
				ReportNode referenceNode;
				ReportNode callFunctionReport = (ReportNode) context.getVariablesManager().getVariable("callReport");
				if(callFunctionReport==null) {
					referenceNode = node;
				} else {
					referenceNode = callFunctionReport;
				}
				ReportNode parentNode = context.getReportNodeCache().get(referenceNode.getParentID().toString());
				varMan.putVariable(parentNode, testArtefact.getKey().get(), result);
			}
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Set testArtefact) {
		return new ReportNode();
	}
}
