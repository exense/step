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

import step.artefacts.Echo;
import step.artefacts.reports.EchoReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class EchoHandler extends ArtefactHandler<Echo, EchoReportNode> {
	
	@Override
	protected void createReportSkeleton_(EchoReportNode parentNode, Echo testArtefact) {

	}

	@Override
	protected void execute_(EchoReportNode node, Echo testArtefact) {
		if(testArtefact.getText() != null && testArtefact.getText().get() != null)
			node.setEcho(testArtefact.getText().get().toString());
		else
			node.setEcho("null value"); //node.setEcho(null);
		node.setStatus(ReportNodeStatus.PASSED);		
	}

	@Override
	public EchoReportNode createReportNode_(ReportNode parentNode, Echo testArtefact) {
		return new EchoReportNode();
	}
}
