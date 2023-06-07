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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.Test;

import junit.framework.Assert;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.model.ExecutionStatus;
import step.threadpool.ThreadPool.WorkerController;

public class ThreadPoolTest {

	protected ExecutionContext newExecutionContext() {
		return ExecutionEngine.builder().build().newExecutionContext();
	}
	
	@Test
	public void test() {
		ExecutionContext context = newExecutionContext();
		ThreadPool threadPool = new ThreadPool(context);
		
		List<String> itemList = Arrays.asList("item1", "item2", "item3");
		
		List<String> processedItems = new ArrayList<>();
		
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item -> processedItems.add(item);
			}
		}, 1, OptionalInt.empty());
		
		Assert.assertEquals(itemList, processedItems);
	}
	
	@Test
	public void testInterrupt() {
		ExecutionContext context = newExecutionContext();
		ThreadPool threadPool = new ThreadPool(context);
		
		List<String> itemList = Arrays.asList("item1", "item2", "item3");
		
		List<String> processedItems = new ArrayList<>();
		
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item -> {
					processedItems.add(item);
					if(item.equals("item2")) {
						control.interrupt();
					}
				};
			}
		}, 1, OptionalInt.empty());
		
		Assert.assertEquals(Arrays.asList("item1", "item2"), processedItems);
	}
	
	@Test
	public void testContextInterrupt() {
		ExecutionContext context = newExecutionContext();
		ThreadPool threadPool = new ThreadPool(context);
		
		List<String> itemList = Arrays.asList("item1", "item2", "item3");
		
		List<String> processedItems = new ArrayList<>();
		
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item -> {
					processedItems.add(item);
					if(item.equals("item2")) {
						context.updateStatus(ExecutionStatus.ABORTING);
					}
				};
			}
		}, 1, OptionalInt.empty());
		
		Assert.assertEquals(Arrays.asList("item1", "item2"), processedItems);
	}

	@Test
	public void testParallel() {
		ExecutionContext context = newExecutionContext();
		ThreadPool threadPool = new ThreadPool(context);
		
		List<String> itemList = new ArrayList<>();
		int iterations = 10000;
		for(int i=0; i<iterations; i++) {
			itemList.add("Item"+i);
		}
		
		List<String> processedItems = new CopyOnWriteArrayList<>();
		
		AtomicInteger count = new AtomicInteger();
		
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item -> {
					processedItems.add(item);
					count.incrementAndGet();
				};
			}
		}, 5, OptionalInt.empty());
		
		for (String item : itemList) {
			if(!processedItems.contains(item)) {
				fail("The item "+item+" hasn't been processed");
			}
		}
		
		// Ensure that each item has been processed exactly once
		assertEquals(iterations, count.get());
	}
	
	@Test
	public void testAutoMode() {
		ExecutionContext context = newExecutionContext();
		ReportNode rootReportNode = context.getReport();
		context.getVariablesManager().putVariable(rootReportNode, "execution_threads_auto", 2);
		
		ThreadPool threadPool = new ThreadPool(context);
		
		List<String> itemList = new ArrayList<>();
		for(int i=0; i<100; i++) {
			itemList.add("Item"+i);
		}
		
		List<String> itemList2 = new ArrayList<>();
		for(int i=0; i<100; i++) {
			itemList2.add(Integer.toString(i));
		}
		
		List<String> processedItems = new CopyOnWriteArrayList<>();
		
		CountDownLatch countDownLatch = new CountDownLatch(2);
		
		ConcurrentHashMap<String,String> threadIdLevel1 = new ConcurrentHashMap<>();
		ConcurrentHashMap<String,String> threadIdLevel2 = new ConcurrentHashMap<>();
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item1 -> {
					context.setCurrentReportNode(rootReportNode);
					threadIdLevel1.put(Thread.currentThread().getName(),"");
					waitForOtherWorkersToStart(countDownLatch);
					threadPool.consumeWork(itemList2.iterator(), new WorkerItemConsumerFactory<String>() {
						@Override
						public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
							return item2 -> {
								threadIdLevel2.put(Thread.currentThread().getName(),"");
								processedItems.add(item1+item2);
							};
						}
					}, 4, OptionalInt.empty());
				};
			}
		}, 4, OptionalInt.empty());
		
		Assert.assertEquals(2, threadIdLevel1.size());
		Assert.assertEquals(2, threadIdLevel2.size());
		for (String item : itemList) {
			for (String item2 : itemList2) {
				String concatenatedItem = item+item2;
				if(!processedItems.contains(concatenatedItem)) {
					fail("The item "+concatenatedItem+" hasn't been processed");
				}
				
			}
		}
	}

	@Test
	public void testAutoModeForTestSetWithSingleTestcase() {
		ExecutionContext context = newExecutionContext();
		ReportNode rootReportNode = context.getReport();
		context.getReportNodeCache().put(rootReportNode);
		context.getVariablesManager().putVariable(rootReportNode, "execution_threads_auto", 2);

		ThreadPool threadPool = new ThreadPool(context);

		List<String> itemList = new ArrayList<>();
		for(int i=0; i<100; i++) {
			itemList.add("Item"+i);
		}

		List<String> itemList2 = new ArrayList<>();
		for(int i=0; i<100; i++) {
			itemList2.add(Integer.toString(i));
		}

		List<String> itemList3 = new ArrayList<>();
		for(int i=0; i<100; i++) {
			itemList3.add(Integer.toString(i));
		}

		List<String> processedItems = new CopyOnWriteArrayList<>();

		CountDownLatch countDownLatch = new CountDownLatch(2);

		AtomicInteger workers1 = new AtomicInteger(0);
		AtomicInteger workers2 = new AtomicInteger(0);
		AtomicInteger workers3 = new AtomicInteger(0);
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item1 -> {
					ReportNode childReportNode1 = new ReportNode();
					context.getReportNodeCache().put(childReportNode1);
					childReportNode1.setParentID(rootReportNode.getId());
					context.setCurrentReportNode(childReportNode1);
					workers1.updateAndGet(x -> x < control.getWorkerId() ? control.getWorkerId() : x);
					waitForOtherWorkersToStart(countDownLatch);
					threadPool.consumeWork(itemList2.iterator(), new WorkerItemConsumerFactory<String>() {
						@Override
						public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
							return item2 -> {
								ReportNode childReportNode2 = new ReportNode();
								childReportNode2.setParentID(childReportNode1.getId());
								context.getReportNodeCache().put(childReportNode2);
								context.setCurrentReportNode(childReportNode2);
								workers2.updateAndGet(x -> x < control.getWorkerId() ? control.getWorkerId() : x);
								processedItems.add(item1+item2);
								threadPool.consumeWork(itemList3.iterator(), new WorkerItemConsumerFactory<String>() {
									@Override
									public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
										return item3 -> {
											ReportNode childReportNode3 = new ReportNode();
											childReportNode3.setParentID(childReportNode3.getId());
											context.getReportNodeCache().put(childReportNode3);
											context.setCurrentReportNode(childReportNode3);
											workers3.updateAndGet(x -> x < control.getWorkerId() ? control.getWorkerId() : x);
										};
									}
								}, 4, OptionalInt.empty());
							};
						}
					}, 4, OptionalInt.empty());
				};
			}
		}, 4, OptionalInt.of(1));

		Assert.assertEquals(0, workers1.get());
		Assert.assertEquals(1, workers2.get());
		Assert.assertEquals(0, workers3.get());
		for (String item : itemList) {
			for (String item2 : itemList2) {
				String concatenatedItem = item+item2;
				if(!processedItems.contains(concatenatedItem)) {
					fail("The item "+concatenatedItem+" hasn't been processed");
				}

			}
		}
	}
	
	protected void waitForOtherWorkersToStart(CountDownLatch countDownLatch) {
		countDownLatch.countDown();
		try {
			countDownLatch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
		}
	}
	
	@Test
	public void testAutoModeDisabled() {
		ExecutionContext context = newExecutionContext();
		
		// Empty string => disabled
		ReportNode rootReportNode = context.getReport();
		context.getVariablesManager().putVariable(rootReportNode, "execution_threads_auto", "");
		
		ThreadPool threadPool = new ThreadPool(context);
		
		List<String> itemList = new ArrayList<>();
		for(int i=0; i<4; i++) {
			itemList.add("Item"+i);
		}
		
		List<String> itemList2 = new ArrayList<>();
		for(int i=0; i<100; i++) {
			itemList2.add(Integer.toString(i));
		}
		
		List<String> processedItems = new CopyOnWriteArrayList<>();
		
		CountDownLatch countDownLatch = new CountDownLatch(4);
		CountDownLatch countDownLatch2 = new CountDownLatch(16);
		
		ConcurrentHashMap<String,String> threadIdLevel1 = new ConcurrentHashMap<>();
		ConcurrentHashMap<String,String> threadIdLevel2 = new ConcurrentHashMap<>();
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item1 -> {
					context.setCurrentReportNode(rootReportNode);
					threadIdLevel1.put(Thread.currentThread().getName(),"");
					waitForOtherWorkersToStart(countDownLatch);
					threadPool.consumeWork(itemList2.iterator(), new WorkerItemConsumerFactory<String>() {
						@Override
						public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
							return item2 -> {
								threadIdLevel2.put(Thread.currentThread().getName(),"");
								waitForOtherWorkersToStart(countDownLatch2);
								processedItems.add(item1+item2);
							};
						}
					}, 4, OptionalInt.empty());
				};
			}
		}, 4, OptionalInt.empty());
		
		Assert.assertEquals(4, threadIdLevel1.size());
		Assert.assertEquals(16, threadIdLevel2.size());
		for (String item : itemList) {
			for (String item2 : itemList2) {
				String concatenatedItem = item+item2;
				if(!processedItems.contains(concatenatedItem)) {
					fail("The item "+concatenatedItem+" hasn't been processed");
				}
				
			}
		}
	}
}
