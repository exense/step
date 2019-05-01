package step.threadpool;

import java.util.function.Consumer;

import step.threadpool.ThreadPool.WorkerController;

public interface WorkerItemConsumerFactory<T> {

	public Consumer<T> createWorkItemConsumer(WorkerController<T> control);
	
}
