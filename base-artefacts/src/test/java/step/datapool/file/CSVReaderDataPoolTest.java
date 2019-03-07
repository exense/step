/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.datapool.file;

import java.io.File;

import org.junit.Test;

import junit.framework.Assert;
import step.commons.helpers.FileHelper;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ContextBuilder;
import step.core.variables.SimpleStringMap;
import step.datapool.DataPoolFactory;
import step.datapool.DataSet;

public class CSVReaderDataPoolTest {

	@Test
	public void testCSVReaderDataPool() {		
		DataSet<?> pool = getDataPool("File.csv");
		Assert.assertEquals("row12", ((SimpleStringMap)pool.next().getValue()).get("Col2").toString());
		// Test empty string
		Assert.assertEquals("", ((SimpleStringMap)pool.next().getValue()).get("Col4").toString());
		pool.close();
	}
	
	@Test
	public void testCSVReaderDataPoolPut() {		
		DataSet<?> pool = getDataPool("File.csv");
		Exception e = null;
		try {
			((SimpleStringMap)pool.next().getValue()).put("test", "test");			
		} catch(Exception ex) {
			e = ex;
		}
		Assert.assertEquals("Put into a CSVRowWrapper row is currently not supported.", e.getMessage());
		pool.close();
	}
	
	@Test
	public void testCSVReaderDataPoolToString() {		
		DataSet<?> pool = getDataPool("File.csv");
		Assert.assertEquals("Col1=row11 Col2=row12 Col3=row13 Col4= Col5=", ((SimpleStringMap)pool.next().getValue()).toString());
		pool.close();
	}

	protected DataSet<?> getDataPool(String file) {
		FileDataPool conf = getCSVDataSourceConf(file);
		DataSet<?> pool =  DataPoolFactory.getDataPool("csv", conf, ContextBuilder.createLocalExecutionContext());
		pool.init();
		return pool;
	}
	
	private CSVDataPool getCSVDataSourceConf(String filename) {
		File file = FileHelper.getClassLoaderResource(this.getClass(), filename);
		
		CSVDataPool conf = new CSVDataPool();
		conf.setFile(new DynamicValue<String>(file.getAbsolutePath()));
		return conf;
	}
}
