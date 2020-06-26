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
import ch.exense.commons.io.FileHelper;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContextBuilder;
import step.datapool.DataPoolFactory;
import step.datapool.DataSet;

public class FlatFileReaderDataPoolTest {

	@Test
	public void testFlatFileReaderDataPool() {		
		DataSet<?> pool = getDataPool("File.txt","file");
		Assert.assertEquals("Line 1", pool.next().getValue());
		pool.close();
	}
	
	protected DataSet<?> getDataPool(String file, String type) {
		FileDataPool conf = getDataSourceConf(file);
		DataSet<?> pool =  DataPoolFactory.getDataPool(type, conf, new ExecutionContextBuilder().configureForlocalExecution().build());
		pool.init();
		return pool;
	}
	
	private FileDataPool getDataSourceConf(String filename) {
		File file = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), filename);
		
		FileDataPool conf = new FileDataPool();
		conf.setFile(new DynamicValue<String>(file.getAbsolutePath()));
		return conf;
	}
}
