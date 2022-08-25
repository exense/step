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
package step.controller.services.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncTaskManager implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskManager.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Map<String, AsyncTaskStatus> tasks = new ConcurrentHashMap<>();

    public <T> AsyncTaskStatus<T> scheduleAsyncTask(AsyncTask<T> asyncTask) {
        String taskId = UUID.randomUUID().toString();
        AsyncTaskStatus status = new AsyncTaskStatus(taskId);
        tasks.put(taskId, status);

        AsyncTaskHandle taskHandle = new AsyncTaskHandle(status);

        executorService.submit(() -> {
            T result = null;
            try {
                result = asyncTask.apply(taskHandle);
                status.setResult(result);
            } catch (Exception e) {
                logger.error("Error while running async task " + taskId, e);
                status.setError(e.getMessage());
            } finally {
                status.setReady(true);
            }
        });
        return status;
    }

    @Override
    public void close() throws IOException {
        executorService.shutdown();
    }

    public AsyncTaskStatus getAsyncTaskStatus(String id) {
        return tasks.get(id);
    }

    public AsyncTaskStatus removeReadyAsyncTaskStatus(String id) {
        AsyncTaskStatus asyncTaskStatus = getAsyncTaskStatus(id);
        if (asyncTaskStatus.isReady()) {
            tasks.remove(id);
        }
        return asyncTaskStatus;
    }

    public Collection<AsyncTaskStatus> getCurrentAsyncTasks() {
        return tasks.values();
    }

    public static class AsyncTaskHandle {

        private final AsyncTaskStatus asyncTaskStatus;

        public AsyncTaskHandle(AsyncTaskStatus asyncTaskStatus) {
            this.asyncTaskStatus = asyncTaskStatus;
        }

        public void setWarnings(Set<String> warnings) {
            asyncTaskStatus.setWarnings(warnings);
        }
    }

}
