package step.threadpool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class ThreadPool {
	
	private static final String EXECUTION_THREADS_AUTO = "execution_threads_auto";

	private static final Logger logger = LoggerFactory.getLogger(ThreadPool.class);

	private final ExecutionContext executionContext;

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	protected ThreadLocal<Stack<BatchContext>> batchContextStack = ThreadLocal.withInitial(()->new Stack<BatchContext>());
	
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
		// Wrapping the iterator to avoid concurrency issues as iterators aren't ThreadSafe 
		Iterator<WORK_ITEM> threadSafeIterator = new Iterator<WORK_ITEM>() {
			@Override
			public boolean hasNext() {
				throw new RuntimeException("This method shouldn't be called");
			}

			@Override
			public WORK_ITEM next() {
				synchronized (this) {
					return workItemIterator.next();
				}
			}
		};
		
		final BatchContext batchContext = new BatchContext(executionContext);
		
		Integer autoNumberOfThreads = getAutoNumberOfThreads();
		if (autoNumberOfThreads != null) {
			if(!isReentrantThread()) {
				// Forcing the number of threads to the required autoNumberOfThreads for the 
				// first Artefact using the ThreadPool (Level = 1)
				numberOfThreads = autoNumberOfThreads;
			} else {
				// Avoid parallelism for the artefacts that are children of an artefact 
				// already using the ThreadPool (Level > 1)
				numberOfThreads = 1;
			}
		}
		
		WorkerController<WORK_ITEM> workerController = new WorkerController<>(batchContext);
		Consumer<WORK_ITEM> workItemConsumer = workItemConsumerFactory.createWorkItemConsumer(workerController);
		
		if(numberOfThreads == 1) {
			// No parallelism, run the worker in the current thread
			createWorkerAndRun(batchContext, workItemConsumer, threadSafeIterator);
		} else {
			List<Future<?>> futures = new ArrayList<>();
			// Create one worker for each "thread"
			for (int i = 0; i < numberOfThreads; i++) {
				futures.add(executorService.submit(() -> {
					executionContext.associateThread();
					createWorkerAndRun(batchContext, workItemConsumer, threadSafeIterator);
				}));
			}
			
			// Wait for the workers to complete
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (InterruptedException | ExecutionException e) {
					logger.error("Error while waiting for worker execution to terminate. Execution ID: "+executionContext.getExecutionId(), e);
				}
			}
		}
	}

	/**
	 * @return true if the current thread is a reentrant thread. A Thread is called "reentrant"
	 * when it is already managed by a {@link ThreadPool}
	 */
	protected boolean isReentrantThread() {
		return !batchContextStack.get().isEmpty();
	}

	protected Integer getAutoNumberOfThreads() {
		ReportNode rootReport = executionContext.getReport();
		Object autoNumberOfThreads = executionContext.getVariablesManager().getVariable(rootReport, EXECUTION_THREADS_AUTO, true);
		if(autoNumberOfThreads != null && autoNumberOfThreads.toString().trim().length() > 0) {
			return Integer.parseInt(autoNumberOfThreads.toString());
		} else {
			return null;
		}
		
	}
	
	private <WORK_ITEM> void createWorkerAndRun(BatchContext batchContext, Consumer<WORK_ITEM> workItemConsumer, Iterator<WORK_ITEM> workItemIterator) {
		Stack<BatchContext> stack = pushBatchContextToStack(batchContext);
		try {
			new Worker<WORK_ITEM>(batchContext, workItemConsumer, workItemIterator).run();
		} finally {
			stack.pop();
		}
	}

	protected Stack<BatchContext> pushBatchContextToStack(final BatchContext batchContext) {
		Stack<BatchContext> stack = batchContextStack.get();
		stack.push(batchContext);
		return stack;
	}
}
