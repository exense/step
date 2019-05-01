package step.threadpool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;

public class ThreadPool {
	
	private static final Logger logger = LoggerFactory.getLogger(ThreadPool.class);

	private final ExecutionContext executionContext;

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	public ThreadPool(ExecutionContext context) {
		super();
		this.executionContext = context;
	}
	
	private static final class BatchContext {

		private final ExecutionContext executionContext;
		private final AtomicBoolean isInterrupted = new AtomicBoolean(false);

		public BatchContext(ExecutionContext context) {
			super();
			this.executionContext = context;
		}
	}

	private static final class Worker<T> implements Runnable {

		private final BatchContext batchContext;
		private final Consumer<T> workItemConsumer;
		private final Iterator<T> workItemIterator;

		public Worker(BatchContext batchContext, Consumer<T> workItemConsumer, Iterator<T> workItemIterator) {
			super();
			this.batchContext = batchContext;
			this.workItemConsumer = workItemConsumer;
			this.workItemIterator = workItemIterator;
		}

		@Override
		public void run() {
			T next;
			try {
				while ((next = workItemIterator.next()) != null) {
					workItemConsumer.accept(next);
					// ensure that a retrieved workitem is always consumed.
					// break if necessary after the item has been consumed (The ForBlockHandler for instance rely on this guaranty for row commit)
					if(batchContext.executionContext.isInterrupted() || batchContext.isInterrupted.get()) {
						break;
					}
				}
			} catch (NoSuchElementException e) {
				// Ignore
			}
		}
	}

	public static final class WorkerController<T> {

		private final BatchContext batchContext;

		public WorkerController(BatchContext batchContext) {
			super();
			this.batchContext = batchContext;
		}

		public void interrupt() {
			batchContext.isInterrupted.set(true);
		}
	}

	public <WORK_ITEM> void consumeWork(Iterator<WORK_ITEM> workItemIterator,
			WorkerItemConsumerFactory<WORK_ITEM> workItemConsumerFactory, int numberOfThreads) {
//		Integer autoNumberOfThreads = context.getVariablesManager().getVariableAsInteger("tec_execution_threads", null);
//		if (autoNumberOfThreads != null) {
//			numberOfThreads = autoNumberOfThreads;
//		}

		final BatchContext batchContext = new BatchContext(executionContext);
		
		WorkerController<WORK_ITEM> workerController = new WorkerController<>(batchContext);
		Consumer<WORK_ITEM> workItemConsumer = workItemConsumerFactory.createWorkItemConsumer(workerController);
		
		if(numberOfThreads == 1) {
			new Worker<WORK_ITEM>(batchContext, workItemConsumer, workItemIterator).run();
		} else {
			List<Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < numberOfThreads; i++) {
				futures.add(executorService.submit(() -> {
					executionContext.associateThread();
					new Worker<WORK_ITEM>(batchContext, workItemConsumer, workItemIterator).run();
				}));
			}
			
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (InterruptedException | ExecutionException e) {
					logger.error("Error while waiting for worker execution to terminate. Execution ID: "+executionContext.getExecutionId(), e);
				}
			}
		}
	}
}
