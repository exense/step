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
package step.artefacts;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;

public class ArtefactQueue {

	private final LinkedBlockingQueue<WorkItem> queue = new LinkedBlockingQueue<>();
	private static final WorkItem END_OF_QUEUE = new WorkItem(null);
	
	public Future<ReportNode> add(AbstractArtefact artefact) {
		WorkItem workItem = new WorkItem(artefact);
		queue.add(workItem);
		return workItem;
	}
	
	public void stop() {
		queue.add(END_OF_QUEUE);
	}

	public WorkItem take() throws InterruptedException {
		WorkItem take = queue.take();
		if(take != END_OF_QUEUE) {
			return take;
		} else {
			return null;
		}
	}
	
	public static class WorkItem extends CompletableFuture<ReportNode> {
		
		final AbstractArtefact artefact;

		public WorkItem(AbstractArtefact artefact) {
			super();
			this.artefact = artefact;
		}

		public AbstractArtefact getArtefact() {
			return artefact;
		}
	}
}
