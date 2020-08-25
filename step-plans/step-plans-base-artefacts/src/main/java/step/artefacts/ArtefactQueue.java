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
