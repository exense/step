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
import step.datapool.DataPoolFactory;
import step.datapool.DataSet;

public class DirectoryDataPoolTest {
	
	@Test
	public void testDirectoryDataPoolTestToString() {		
		DataSet<?> pool = getDataPool("folder","folder");
		Assert.assertTrue(pool.next().getValue().toString().contains("File.txt"));
		pool.close();
	}

	protected DataSet<?> getDataPool(String file, String type) {
		DirectoryDataPool conf = getDataSourceConf(file);
		DataSet<?> pool =  DataPoolFactory.getDataPool(type, conf, ContextBuilder.createLocalExecutionContext());
		pool.init();
		return pool;
	}
	
	private DirectoryDataPool getDataSourceConf(String filename) {
		File file = FileHelper.getClassLoaderResource(this.getClass(), filename);
		
		DirectoryDataPool conf = new DirectoryDataPool();
		conf.setFolder(new DynamicValue<String>(file.getAbsolutePath()));
		return conf;
	}
}
