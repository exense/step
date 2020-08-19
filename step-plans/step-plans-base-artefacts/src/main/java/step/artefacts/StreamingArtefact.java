package step.artefacts;

import java.util.concurrent.Future;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.artefacts.ArtefactQueue.WorkItem;
import step.artefacts.handlers.StreamingArtefactHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNode;

@Artefact(handler = StreamingArtefactHandler.class)
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
