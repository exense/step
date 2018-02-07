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

import step.artefacts.Synchronized;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class SynchronizedHandler extends ArtefactHandler<Synchronized, ReportNode> {
	
	private SequenceHandler sh = new SequenceHandler();
	
	@Override
	public void createReportSkeleton_(ReportNode node, Synchronized testArtefact) {
		
		sh.createReportSkeleton_(node, testArtefact);
	}
	
	@Override
	public void execute_(ReportNode node, Synchronized testArtefact) {
		sh.init(context);
		synchronized(context) {
			sh.execute_(node, testArtefact);
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Synchronized testArtefact) {
		return sh.createReportNode_(parentNode, testArtefact);
	}

}
