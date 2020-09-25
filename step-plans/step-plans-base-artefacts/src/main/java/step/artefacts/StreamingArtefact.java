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

import java.util.concurrent.Future;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.artefacts.ArtefactQueue.WorkItem;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNode;

@Artefact()
public class StreamingArtefact extends AbstractArtefact {

	@JsonIgnore
	private ArtefactQueue queue;
	
	public StreamingArtefact() {
		super();
		this.queue = new ArtefactQueue();
	}

	public ArtefactQueue getQueue() {
		return queue;
	}

	public void setQueue(ArtefactQueue queue) {
		this.queue = queue;
	}

	public Future<ReportNode> addToQueue(AbstractArtefact e) {
		return queue.add(e);
	}
	
	public void stopQueue() {
		queue.stop();
	}

	public WorkItem takeFromQueue() throws InterruptedException {
		return queue.take();
	}
	

}
