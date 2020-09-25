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
package step.datapool.file;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import junit.framework.Assert;
import step.artefacts.AbstractArtefactTest;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.runner.PlanRunnerResultAssert;
import step.core.variables.SimpleStringMap;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;

public class CSVReaderDataPoolTest extends AbstractArtefactTest {

	@Test
	public void testEmpty() {
		
		DataSet<?> pool = getDataPool("empty.csv");
		Assert.assertNull(pool.next());
		
		pool.close();
	}
	
	@Test
	public void testCSVReaderDataPool() {		
		DataSet<?> pool = getDataPool("File.csv");
		Assert.assertEquals("row12", ((SimpleStringMap)pool.next().getValue()).get("Col2").toString());
		// Test empty string
		Assert.assertEquals("", ((SimpleStringMap)pool.next().getValue()).get("Col4").toString());
		pool.close();
	}

	
	@Test
	public void testCSVReaderDataPoolPut() throws IOException {		
		File tempFile = FileHelper.extractResourceToTempFile(this.getClass(), "testCSVReaderDataPoolPut.csv");
		
		DataSet<?> pool = getDataPool(tempFile, true);
		
		ExecutorService threadPool = Executors.newCachedThreadPool();
		for(int i=0;i<5;i++) {
			threadPool.submit(() -> {
				DataPoolRow row = null;
				while((row=pool.next())!=null) {
					try {
						((SimpleStringMap)row.getValue()).put("Col4", "test");			
					} finally {
						if(row != null) {
							row.commit();
						}
					}
				}
			});
		}
		pool.close();
		
		PlanRunnerResultAssert.assertEquals(getClass(), "testCSVReaderDataPoolPut.expected.csv", tempFile);
	}
	
	@Test
	public void testCSVReaderDataPoolToString() {		
		DataSet<?> pool = getDataPool("File.csv");
		Assert.assertEquals("Col1=row11 Col2=row12 Col3=row13 Col4= Col5=", ((SimpleStringMap)pool.next().getValue()).toString());
		pool.close();
	}
	
	protected DataSet<?> getDataPool(String filename) {
		File file = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), filename);
		return getDataPool(file, false);
	}
	
	protected DataSet<?> getDataPool(File file, boolean enableRowCommit) {
		FileDataPool conf = getCSVDataSourceConf(file);
		DataSet<?> pool =  DataPoolFactory.getDataPool("csv", conf, newExecutionContext());
		pool.enableRowCommit(enableRowCommit);
		pool.init();
		return pool;
	}
	
	private CSVDataPool getCSVDataSourceConf(File file) {
		CSVDataPool conf = new CSVDataPool();
		conf.setFile(new DynamicValue<String>(file.getAbsolutePath()));
		return conf;
	}
}
