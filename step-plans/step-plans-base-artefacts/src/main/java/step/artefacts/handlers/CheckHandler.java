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

import step.artefacts.Check;
import step.artefacts.reports.CheckReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class CheckHandler extends ArtefactHandler<Check, CheckReportNode> {
	
	@Override
	protected void createReportSkeleton_(CheckReportNode parentNode, Check testArtefact) {

	}

	@Override
	protected void execute_(CheckReportNode node, Check testArtefact) {
		ReportNode callFunctionReport = (ReportNode) context.getVariablesManager().getVariable("callReport"); 
		if(callFunctionReport==null || callFunctionReport.getStatus()==ReportNodeStatus.PASSED) {
			if(testArtefact.getExpression().get()) {
				node.setStatus(ReportNodeStatus.PASSED);
			} else {
				node.setStatus(ReportNodeStatus.FAILED);
				node.addError("The expression '" + testArtefact.getExpression().getExpression() + "' returned false");
			}
		} else {
			node.setStatus(ReportNodeStatus.NORUN);
		}
	}

	@Override
	public CheckReportNode createReportNode_(ReportNode parentNode, Check testArtefact) {
		return new CheckReportNode();
	}
}
