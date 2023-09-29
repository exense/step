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

import step.artefacts.TestCase;
import step.artefacts.reports.TestCaseReportNode;
import step.core.artefacts.handlers.AtomicReportNodeStatusComposer;
import step.core.artefacts.handlers.ReportNodeAttributesManager;
import step.core.artefacts.reports.ReportNode;

public class TestCaseHandler extends AbstractSessionArtefactHandler<TestCase, ReportNode> {
	
	@Override
	public void createReportSkeleton_(ReportNode node, TestCase testArtefact) {
		addTestCaseNameToCustomAttributes(testArtefact);
		createReportNodeSkeletonInSession(testArtefact, node, (sessionArtefact, sessionReportNode)->{
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
			scheduler.createReportSkeleton_(sessionReportNode, sessionArtefact);
		});
	}
	
	@Override
	public void execute_(ReportNode node, TestCase testArtefact) {
		addTestCaseNameToCustomAttributes(testArtefact);
		ReportNode resultNode = executeInSession(testArtefact, node, (sessionArtefact, sessionReportNode)->{
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
			scheduler.execute_(sessionReportNode, sessionArtefact);
		});
		new AtomicReportNodeStatusComposer(resultNode).applyComposedStatusToParentNode(node);
	}
	
	private void addTestCaseNameToCustomAttributes(TestCase testArtefact) {
		ReportNodeAttributesManager reportNodeAttributesManager = new ReportNodeAttributesManager(context);
		reportNodeAttributesManager.addCustomAttribute("TestCase", testArtefact.getId().toString());
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestCase testArtefact) {
		return new TestCaseReportNode();
	}

}
