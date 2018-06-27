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

import step.artefacts.RetryIfFails;
import step.artefacts.Sequence;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class RetryIfFailsHandler extends ArtefactHandler<RetryIfFails, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode, RetryIfFails testArtefact) {
		ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
		Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration"+1, true);
		delegateCreateReportSkeleton(iterationTestCase, parentNode);
	}

	@Override
	protected void execute_(ReportNode node, RetryIfFails testArtefact) {
		boolean success = false;
		
		long begin = System.currentTimeMillis();
		
		for(int count = 1; count<=testArtefact.getMaxRetries().get();count++) {
			ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
			Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration"+count, true);
			
			ReportNode iterationReportNode = delegateExecute(iterationTestCase, node);
			
			if(iterationReportNode.getStatus()==ReportNodeStatus.PASSED) {
				success = true;
			}
			
			if(iterationReportNode.getStatus()==ReportNodeStatus.PASSED || context.isInterrupted()) {
				break;
			}
			
			if(testArtefact.getTimeout().get() > 0 && System.currentTimeMillis() > (begin + testArtefact.getTimeout().get())){
				break;
			}
			
			try {
				Thread.sleep(testArtefact.getGracePeriod().get());
			} catch (InterruptedException e) {}
		}
		
		node.setStatus(success?ReportNodeStatus.PASSED:ReportNodeStatus.FAILED);
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, RetryIfFails testArtefact) {
		return new ReportNode();
	}

}
