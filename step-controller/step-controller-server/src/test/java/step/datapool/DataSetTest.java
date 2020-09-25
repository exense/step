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
package step.datapool;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;

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
		dataSet.setContext(newExecutionContext());
		dataSet.init();
		// Test that the data set can be closed without error
		dataSet.close();
	}

	protected ExecutionContext newExecutionContext() {
		return ExecutionEngine.builder().build().newExecutionContext();
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
		dataSet.setContext(newExecutionContext());
		dataSet.init();
		dataSet.next().commit();
		dataSet.next().commit();
		dataSet.close();
		
		Assert.assertEquals(2, writeRowCallCount.get());
	}

}
