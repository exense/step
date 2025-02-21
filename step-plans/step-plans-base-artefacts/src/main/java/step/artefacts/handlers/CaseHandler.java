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

import step.artefacts.Case;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.SequentialArtefactScheduler;
import step.core.artefacts.reports.ReportNode;

public class CaseHandler extends ArtefactHandler<Case, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode,
			Case testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
		scheduler.createReportSkeleton_(parentNode, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, Case testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
		scheduler.execute_(node, testArtefact);
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Case testArtefact) {
		return new ReportNode();
	}

}
