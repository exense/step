package step.controller.services.async;

import ch.exense.commons.io.Poller;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class AsyncTaskManagerTest {

    private static final String TEST = "test";

    @Test
    void test() throws InterruptedException, TimeoutException, IOException {
        CountDownLatch latch = new CountDownLatch(1);
        AsyncTaskManager asyncTaskManager = new AsyncTaskManager();
        AsyncTaskStatus<Object> task = asyncTaskManager.scheduleAsyncTask(h -> {
            latch.await();
            h.setWarnings(Set.of("My Warning"));
            return TEST;
        });
        String id = task.getId();

        assertFalse(task.isReady());
        assertNull(task.getResult());
        assertEquals(0, task.getProgress());

        latch.countDown();

        // Wait for the task to be ready
        Poller.waitFor(() -> asyncTaskManager.getAsyncTaskStatus(id).isReady(), 1000);

        Collection<AsyncTaskStatus> currentAsyncTasks = asyncTaskManager.getCurrentAsyncTasks();
        assertEquals(1, currentAsyncTasks.size());

        // Remove the ready task
        task = asyncTaskManager.removeReadyAsyncTaskStatus(id);
        // Assert it is ready
        assertTrue(task.isReady());
        assertEquals(TEST, task.getResult());
        assertEquals(Set.of("My Warning"), task.getWarnings());

        // The task should have been removed after calling removeReadyAsyncTaskStatus
        assertNull(asyncTaskManager.getAsyncTaskStatus(id));

        currentAsyncTasks = asyncTaskManager.getCurrentAsyncTasks();
        assertEquals(0, currentAsyncTasks.size());

        asyncTaskManager.close();
    }

    @Test
    void testError() throws InterruptedException, TimeoutException, IOException {
        AsyncTaskManager asyncTaskManager = new AsyncTaskManager();
        AsyncTaskStatus<Object> task = asyncTaskManager.scheduleAsyncTask(h -> {
            throw new RuntimeException("My error");
        });
        String id = task.getId();

        // Wait for the task to be ready
        Poller.waitFor(() -> asyncTaskManager.getAsyncTaskStatus(id).isReady(), 1000);

        task = asyncTaskManager.getAsyncTaskStatus(id);
        assertTrue(task.isReady());
        assertNull(task.getResult());
        assertEquals("My error", task.getError());
    }
}