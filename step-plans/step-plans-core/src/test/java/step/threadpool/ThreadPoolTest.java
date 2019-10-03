package step.threadpool;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Test;

import junit.framework.Assert;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionStatus;
import step.threadpool.ThreadPool.WorkerController;

public class ThreadPoolTest {

	@Test
	public void test() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
		ThreadPool threadPool = new ThreadPool(context);
		
		List<String> itemList = Arrays.asList("item1", "item2", "item3");
		
		List<String> processedItems = new ArrayList<>();
		
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item -> processedItems.add(item);
			}
		}, 1);
		
		Assert.assertEquals(itemList, processedItems);
	}
	
	@Test
	public void testInterrupt() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
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
		}, 1);
		
		Assert.assertEquals(Arrays.asList("item1", "item2"), processedItems);
	}
	
	@Test
	public void testContextInterrupt() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
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
		}, 1);
		
		Assert.assertEquals(Arrays.asList("item1", "item2"), processedItems);
	}

	@Test
	public void testParallel() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
		ThreadPool threadPool = new ThreadPool(context);
		
		List<String> itemList = new ArrayList<>();
		for(int i=0; i<1000; i++) {
			itemList.add("Item"+i);
		}
		
		List<String> processedItems = new CopyOnWriteArrayList<>();
		
		threadPool.consumeWork(itemList.iterator(), new WorkerItemConsumerFactory<String>() {
			@Override
			public Consumer<String> createWorkItemConsumer(WorkerController<String> control) {
				return item -> processedItems.add(item);
			}
		}, 4);
		
		for (String item : itemList) {
			if(!processedItems.contains(item)) {
				fail("The item "+item+" hasn't been processed");
			}
		}
	}
	
	@Test
	public void testAutoMode() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
		context.getVariablesManager().putVariable(context.getReport(), "execution_threads_auto", 2);
		
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
					}, 4);
				};
			}
		}, 4);
		
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
	
	protected void waitForOtherWorkersToStart(CountDownLatch countDownLatch) {
		countDownLatch.countDown();
		try {
			countDownLatch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
		}
	}
	
	@Test
	public void testAutoModeDisabled() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
		// Empty string => disabled
		context.getVariablesManager().putVariable(context.getReport(), "execution_threads_auto", "");
		
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
					}, 4);
				};
			}
		}, 4);
		
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
