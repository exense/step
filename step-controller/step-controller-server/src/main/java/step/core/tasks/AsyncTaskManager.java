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
package step.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AsyncTaskManager implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(AsyncTaskManager.class);

	private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
	private final ExecutorService executorService = Executors.newCachedThreadPool();
	
	public TaskStatus createTask(Consumer<TaskContext> task) {
		String exportId = UUID.randomUUID().toString();
		return createTask(exportId, task);
	}
	
	public TaskStatus createTask(String exportId, Consumer<TaskContext> task) {
		TaskStatus status = new TaskStatus(exportId);
		taskStatusMap.put(exportId, status);
		executorService.submit(() -> {
			try {
				TaskContext taskContext = new TaskContext(status);
				task.accept(taskContext);
			} catch(Exception e) {
				logger.error("Error while running export task "+exportId, e);
			} finally {
				status.ready = true;
			}

		});
		return status;
	}
	
	public TaskStatus getTaskStatus(String taskId) {
		TaskStatus export = taskStatusMap.get(taskId);
		if(export.ready) {
			taskStatusMap.remove(taskId);
		}
		return export;
	}

	@Override
	public void close() throws IOException {
		executorService.shutdown();
	}

	public static class TaskStatus {
		
		private String id;

		private volatile boolean ready = false;

		private volatile float progress = 0;

		private Set<String> warnings;

		private Object result;

		public TaskStatus(String id) {
			super();
			this.id = id;
			this.warnings = new HashSet<>();
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
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

		public Object getResult() {
			return result;
		}
	}

	public static class TaskContext {

		private final TaskStatus exportStatus;

		private TaskContext(TaskStatus exportStatus) {
			this.exportStatus = exportStatus;
		}

		public void setProgress(float progress) {
			exportStatus.setProgress(progress);
		}

		public void addWarning(String warning) {
			exportStatus.getWarnings().add(warning);
		}

		public void setResult(Object result) {
			exportStatus.result = result;
		}
	}
}
