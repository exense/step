package step.threadpool;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
}
