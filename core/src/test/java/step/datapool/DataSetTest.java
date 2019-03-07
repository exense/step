package step.datapool;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.core.execution.ContextBuilder;

public class DataSetTest {

	@Test
	public void test() {
		DataSet<Object> dataSet = new DataSet<Object>(new Object()) {

			@Override
			public void reset() {
				
			}

			@Override
			public Object next_() {
				return null;
			}

			@Override
			public void addRow(Object row) {
				
			}
		};
		dataSet.setContext(ContextBuilder.createLocalExecutionContext());
		dataSet.init();
		// Test that the data set can be closed without error
		dataSet.close();
	}
	
	@Test
	public void testWriteRow() {
		AtomicInteger writeRowCallCount = new AtomicInteger(0);
		DataSet<Object> dataSet = new DataSet<Object>(new Object()) {

			@Override
			public void writeRow(DataPoolRow row) throws IOException {
				writeRowCallCount.incrementAndGet();
			}

			@Override
			public void reset() {
				
			}

			@Override
			public Object next_() {
				return "dummy";
			}

			@Override
			public void addRow(Object row) {
				
			}
		};
		dataSet.setContext(ContextBuilder.createLocalExecutionContext());
		dataSet.init();
		dataSet.next().commit();
		dataSet.next().commit();
		dataSet.close();
		
		Assert.assertEquals(2, writeRowCallCount.get());
	}

}
