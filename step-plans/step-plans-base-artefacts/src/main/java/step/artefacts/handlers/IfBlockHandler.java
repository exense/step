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

import step.artefacts.IfBlock;
import step.artefacts.reports.IfBlockReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

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
		try {
			if(testArtefact.getCondition().get()) {
				SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
				if(execution) {
					scheduler.execute_(node, testArtefact);
				} else {
					scheduler.createReportSkeleton_(node, testArtefact);
				}
			} else {
				node.setStatus(ReportNodeStatus.PASSED);	
			} 
			
		} catch (Exception e) {
			failWithException(node, e);
		}
	}

	@Override
	public IfBlockReportNode createReportNode_(ReportNode parentNode, IfBlock testArtefact) {
		return new IfBlockReportNode();
	}

}
