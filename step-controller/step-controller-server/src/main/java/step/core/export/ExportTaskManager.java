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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.resources.Resource;
import step.resources.ResourceManager;

public class ExportTaskManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportTaskManager.class);

	private final ResourceManager resourceManager;
	private final Map<String, ExportStatus> exportStatusMap = new ConcurrentHashMap<>();
	private final ExecutorService exportExecutor = Executors.newFixedThreadPool(2);
	
	public ExportTaskManager(ResourceManager resourceManager) {
		super();
		this.resourceManager = resourceManager;
	}

	public ExportStatus createExportTask(ExportTask exportTask) {
		String exportId = UUID.randomUUID().toString();
		ExportStatus status = new ExportStatus(exportId);
		exportStatusMap.put(exportId, status);

		ExportTaskHandle taskHandle = new ExportTaskHandle(resourceManager, status);

		exportExecutor.submit(() -> {
			Resource resource = null;
			try {
				resource = exportTask.apply(taskHandle);
			} catch(Exception e) {
				logger.error("Error while running export task "+exportId, e);
			} finally {
				if(resource!=null) {
					status.setAttachmentID(resource.getId().toString());
				}
				status.setReady(true);
			}
		});
		return status;
	}

	public ExportStatus getExportStatus(String exportID) {
		ExportStatus export = exportStatusMap.get(exportID);
		if(export.isReady()) {
			exportStatusMap.remove(exportID);
		}
		return export;
	}

	public Collection<ExportStatus> getCurrentExportStatus() {
		return exportStatusMap.values();
	}

	public static class ExportTaskHandle {

		private final ExportStatus exportStatus;
		private	final ResourceManager resourceManager;

		public ExportTaskHandle(ResourceManager resourceManager, ExportStatus exportStatus) {
			this.resourceManager = resourceManager;
			this.exportStatus = exportStatus;
		}

		public ResourceManager getResourceManager() {
			return resourceManager;
		}

		public void setWarnings(Set<String> warnings) {
			exportStatus.setWarnings(warnings);
		}
	}

}
