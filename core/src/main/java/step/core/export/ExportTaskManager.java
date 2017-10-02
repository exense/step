package step.core.export;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import step.attachments.AttachmentContainer;
import step.attachments.AttachmentManager;

public class ExportTaskManager {
	
	protected AttachmentManager attachmentManager;
	
	protected Map<String, ExportStatus> exportStatusMap = new ConcurrentHashMap<>();
	
	protected ExecutorService exportExecutor = Executors.newFixedThreadPool(2);
	
	public ExportTaskManager(AttachmentManager attachmentManager) {
		super();
		this.attachmentManager = attachmentManager;
	}
	
	public ExportStatus createExportTask(ExportRunnable runnable) {
		String exportId = UUID.randomUUID().toString();
		ExportStatus status = new ExportStatus(exportId);
		exportStatusMap.put(exportId, status);
		AttachmentContainer container = attachmentManager.createAttachmentContainer();
		status.setAttachmentID(container.getMeta().getId().toString());
		runnable.setStatus(status);
		runnable.setContainer(container.getContainer());
		exportExecutor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					runnable.runExport();
				} catch(Exception e) {
					
				} finally {
					status.ready = true;
				}
				
			}
			
		});
		return status;
	}
	
	public static abstract class ExportRunnable {
		
		ExportStatus status;
		
		File container;
		
		public ExportStatus getStatus() {
			return status;
		}

		private void setStatus(ExportStatus status) {
			this.status = status;
		}

		protected File getContainer() {
			return container;
		}

		private void setContainer(File container) {
			this.container = container;
		}
		
		protected abstract void runExport() throws Exception;
		
	}
	
	public ExportStatus getExportStatus(String exportID) {
		ExportStatus export = exportStatusMap.get(exportID);
		if(export.ready) {
			exportStatusMap.remove(exportID);
		}
		return export;
	}
	
	
	public class ExportStatus {
		
		String id;
		
		String attachmentID;
		
		volatile boolean ready = false;
				
		volatile float progress = 0;

		public ExportStatus() {
			super();
		}

		public ExportStatus(String id) {
			super();
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getAttachmentID() {
			return attachmentID;
		}

		public void setAttachmentID(String attachmentID) {
			this.attachmentID = attachmentID;
		}

		public boolean isReady() {
			return ready;
		}

		public void setReady(boolean ready) {
			this.ready = ready;
		}

		public float getProgress() {
			return progress;
		}

		public void setProgress(float progress) {
			this.progress = progress;
		}
	}
}
