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
package step.threadpool;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
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

public class ThreadPool implements Closeable {
	
	private static final String EXECUTION_THREADS_AUTO = "execution_threads_auto";

	private static final String EXECUTION_THREADS_AUTO_CONSUMED = "$execution_threads_auto_consumed";

	private static final Logger logger = LoggerFactory.getLogger(ThreadPool.class);

	private final ExecutionContext executionContext;

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	protected ThreadLocal<Stack<BatchContext>> batchContextStack = ThreadLocal.withInitial(()->new Stack<BatchContext>());
	
	public ThreadPool(ExecutionContext context) {
		super();
		this.executionContext = context;
	}

	@Override
	public void close() throws IOException {
		executorService.shutdown();
	}

	private static final class BatchContext {

		private final ExecutionContext executionContext;
		private final AtomicBoolean isInterrupted = new AtomicBoolean(false);
		private final ThreadLocal<Integer> workerIds = new ThreadLocal<>();
		private final boolean isParallel;
		
		public BatchContext(ExecutionContext executionContext, boolean isParallel) {
			super();
			this.executionContext = executionContext;
			this.isParallel = isParallel;
		}

		public boolean isParallel() {
			return isParallel;
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
		
		public int getWorkerId() {
			return batchContext.workerIds.get();
		}

		/**
		 * @return true if the current batch is running in parallel i.e with more than one worker
		 */
		public boolean isParallel() {
			return batchContext.isParallel();
		}
	}

	public <WORK_ITEM> void consumeWork(Iterator<WORK_ITEM> workItemIterator,
			WorkerItemConsumerFactory<WORK_ITEM> workItemConsumerFactory, int numberOfThreads,
										OptionalInt expectedNumberOfThreads) {
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
		
		Integer autoNumberOfThreads = getAutoNumberOfThreads();
		if (autoNumberOfThreads != null) {
			if(!isAutoNumberOfThreadsConsumed() && expectedNumberOfThreads.orElse(Integer.MAX_VALUE) > 1) {
				// Forcing the number of threads to the required autoNumberOfThreads for the 
				// first Artefact using the ThreadPool (Level = 1)
				numberOfThreads = autoNumberOfThreads;
				consumeAutoNumberOfThreads();
			} else {
				// Avoid parallelism for the artefacts that are children of an artefact 
				// already using the ThreadPool (Level > 1)
				numberOfThreads = 1;
			}
		}
		
		final BatchContext batchContext = new BatchContext(executionContext, numberOfThreads>1);
		
		WorkerController<WORK_ITEM> workerController = new WorkerController<>(batchContext);
		Consumer<WORK_ITEM> workItemConsumer = workItemConsumerFactory.createWorkItemConsumer(workerController);
		
		if(numberOfThreads == 1) {
			// No parallelism, run the worker in the current thread
			createWorkerAndRun(batchContext, workItemConsumer, threadSafeIterator, 0);
		} else {
			ReportNode currentReportNode = executionContext.getCurrentReportNode();
			List<Future<?>> futures = new ArrayList<>();
			long parentThreadId = Thread.currentThread().getId();
			// Create one worker for each "thread"
			for (int i = 0; i < numberOfThreads; i++) {
				int workerId = i;
				futures.add(executorService.submit(() -> {
					executionContext.associateThread(parentThreadId, currentReportNode);
					createWorkerAndRun(batchContext, workItemConsumer, threadSafeIterator, workerId);
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
		Object autoNumberOfThreads = executionContext.getVariablesManager().getVariableAsString(EXECUTION_THREADS_AUTO, null);
		if(autoNumberOfThreads != null && autoNumberOfThreads.toString().trim().length() > 0) {
			return Integer.parseInt(autoNumberOfThreads.toString());
		} else {
			return null;
		}
		
	}

	protected boolean isAutoNumberOfThreadsConsumed() {
		Object autoNumberOfThreads = executionContext.getVariablesManager().getVariableAsString(EXECUTION_THREADS_AUTO_CONSUMED, "false");
		return Boolean.parseBoolean(autoNumberOfThreads.toString());
	}

	protected void consumeAutoNumberOfThreads() {
		executionContext.getVariablesManager().putVariable(executionContext.getCurrentReportNode(), EXECUTION_THREADS_AUTO_CONSUMED, "true");
	}
	
	private <WORK_ITEM> void createWorkerAndRun(BatchContext batchContext, Consumer<WORK_ITEM> workItemConsumer, Iterator<WORK_ITEM> workItemIterator, int workerId) {
		Stack<BatchContext> stack = pushBatchContextToStack(batchContext);
		try {
			batchContext.workerIds.set(workerId);
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
