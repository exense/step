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

import org.apache.commons.lang3.time.DurationFormatUtils;
import step.artefacts.RetryIfFails;
import step.artefacts.Sequence;
import step.artefacts.reports.RetryIfFailsReportNode;
import step.common.managedoperations.OperationManager;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public class RetryIfFailsHandler extends ArtefactHandler<RetryIfFails, RetryIfFailsReportNode> {

	@Override
	protected void createReportSkeleton_(RetryIfFailsReportNode parentNode, RetryIfFails testArtefact) {
		Sequence iterationTestCase = createWorkArtefact(Sequence.class, testArtefact, "Iteration"+1, true);
		delegateCreateReportSkeleton(iterationTestCase, parentNode);
	}

	@Override
	protected void execute_(RetryIfFailsReportNode node, RetryIfFails testArtefact) {

		ReportNode lastReportNode = null;
		ReportNodeStatus lastStatus = ReportNodeStatus.NORUN;
		
		long begin = System.currentTimeMillis();
		boolean inSession = isInSession();
		for(int count = 1; count<=testArtefact.getMaxRetries().get();count++) {
			if(lastReportNode != null) {
				removeErrorContributionsInReportBranch(lastReportNode);
			}
			if (context.isInterrupted()) {
				break;
			}
			boolean persistFail = (!testArtefact.getReportLastTryOnly().get() || 
					(testArtefact.getReportLastTryOnly().get() && count>=testArtefact.getMaxRetries().get()));
			
			node.incTries();
			context.getEventManager().notifyReportNodeUpdated(node);
			
			Sequence iterationTestCase = createWorkArtefact(Sequence.class, testArtefact, "Iteration"+count, true, true);
			
			ReportNode iterationReportNode = delegateExecute(iterationTestCase, node);
			
			lastStatus = iterationReportNode.getStatus();
			lastReportNode = iterationReportNode;

			if (iterationReportNode.getStatus() == ReportNodeStatus.PASSED) {
				break;
			} 
			if(testArtefact.getTimeout().get() > 0 && System.currentTimeMillis() > (begin + testArtefact.getTimeout().get())){
				lastStatus = ReportNodeStatus.FAILED;
				break;
			}
			if (count>=testArtefact.getMaxRetries().get()) {
				break;
			}
			if (!persistFail){
				//Cleanup intermediate results if persist only last results is ON
				node.incSkipped();
				context.getEventManager().notifyReportNodeUpdated(node);
				pruneReportBranch(iterationReportNode);
			}
			
			long duration = testArtefact.getGracePeriod().get();
			boolean releaseToken = (testArtefact.getReleaseTokens().get() && testArtefact.getGracePeriod().get() > 0);
			Map<String,String> details = new LinkedHashMap<>();
			details.put("Grace period", DurationFormatUtils.formatDuration(duration, "HH:mm:ss.SSS"));
			if (inSession) {
				details.put("Release token", Boolean.toString(releaseToken));
			}
			OperationManager.getInstance().enter("RetryIfFails", details , node.getId().toString());
			if (releaseToken && inSession) {
				releaseTokens();
				node.setReleasedToken(true);
			}
			context.getEventManager().notifyReportNodeUpdated(node);
			if (!CancellableSleep.sleep(duration, context::isInterrupted, RetryIfFailsHandler.class)) {
				lastStatus = ReportNodeStatus.INTERRUPTED;
			}
			OperationManager.getInstance().exit();
		}
		
		node.setStatus(lastStatus);
	}

	@Override
	public RetryIfFailsReportNode createReportNode_(ReportNode parentNode, RetryIfFails testArtefact) {
		return new RetryIfFailsReportNode();
	}

}
