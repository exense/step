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
package step.core.export;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.core.model.resources.Resource;
import step.resources.ResourceManager;

public class ExportTaskManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportTaskManager.class);

	protected ResourceManager resourceManager;
	
	protected Map<String, ExportStatus> exportStatusMap = new ConcurrentHashMap<>();
	
	protected ExecutorService exportExecutor = Executors.newFixedThreadPool(2);
	
	public ExportTaskManager(ResourceManager resourceManager) {
		super();
		this.resourceManager = resourceManager;
	}
	
	public ExportStatus createExportTask(ExportRunnable runnable) {
		String exportId = UUID.randomUUID().toString();
		return createExportTask(exportId, runnable);
	}
	
	public ExportStatus createExportTask(String exportId, ExportRunnable runnable) {
		ExportStatus status = new ExportStatus(exportId);
		exportStatusMap.put(exportId, status);
		runnable.setStatus(status);
		runnable.setResourceManager(resourceManager);
		exportExecutor.submit(new Runnable() {

			@Override
			public void run() {
				Resource resource = null;
				try {
					resource = runnable.runExport();
				} catch(Exception e) {
					logger.error("Error while running export task "+exportId, e);
				} finally {
					if(resource!=null) {
						status.setAttachmentID(resource.getId().toString());
					}
					status.ready = true;
				}
				
			}
			
		});
		return status;
	}
	
	public static abstract class ExportRunnable {
		
		ExportStatus status;
		
		private	ResourceManager resourceManager;
		
		public ExportStatus getStatus() {
			return status;
		}

		private void setResourceManager(ResourceManager resourceManager) {
			this.resourceManager = resourceManager;
		}

		protected ResourceManager getResourceManager() {
			return resourceManager;
		}

		private void setStatus(ExportStatus status) {
			this.status = status;
		}
		
		protected abstract Resource runExport() throws Exception;
		
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
		
		Set<String> warnings;

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

		public Set<String> getWarnings() {
			return warnings;
		}

		public void setWarnings(Set<String> warnings) {
			this.warnings = warnings;
		}
	}
}
