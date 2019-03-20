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
import step.core.execution.ExecutionContext;

public class SynchronizedHandler extends ArtefactHandler<Synchronized, ReportNode> {
	
	private static final String LOCK_OBJECT_KEY = "$synchronizedHandlerLockObject";
	
	private SequenceHandler sh = new SequenceHandler();
	
	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		synchronized (LOCK_OBJECT_KEY) {
			if(context.get(LOCK_OBJECT_KEY)==null) {
				context.put(LOCK_OBJECT_KEY, new Object());
			}
		}
	}

	@Override
	public void createReportSkeleton_(ReportNode node, Synchronized testArtefact) {
		sh.createReportSkeleton_(node, testArtefact);
	}
	
	@Override
	public void execute_(ReportNode node, Synchronized testArtefact) {
		sh.init(context);
		Object lock = context.get(LOCK_OBJECT_KEY);
		synchronized(lock) {
			sh.execute_(node, testArtefact);
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Synchronized testArtefact) {
		return sh.createReportNode_(parentNode, testArtefact);
	}

}
