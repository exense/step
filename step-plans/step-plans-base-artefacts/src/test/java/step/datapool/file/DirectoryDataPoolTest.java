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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import junit.framework.Assert;
import step.artefacts.AbstractArtefactTest;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;
import step.datapool.file.FileDataPoolImpl.ExtendedFile;

public class DirectoryDataPoolTest extends AbstractArtefactTest {
	
	private DataSet<?> pool;
	
	private File file;
	
	@Before
	public void before() {
		file = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "folder");
		
		DirectoryDataPool conf = new DirectoryDataPool();
		conf.setFolder(new DynamicValue<String>(file.getAbsolutePath()));

		pool =  DataPoolFactory.getDataPool("folder", conf, newExecutionContext());
		pool.init();
	}
	
	@After
	public void after() {
		pool.close();
	}

	@Test
	public void testEmpty() {

		File fileEmpty = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "emptyFolder");
		
		DirectoryDataPool conf = new DirectoryDataPool();
		conf.setFolder(new DynamicValue<String>(fileEmpty.getAbsolutePath()));

		DataSet<?> poolEmpty = DataPoolFactory.getDataPool("folder", conf, newExecutionContext());
		poolEmpty.init();

		DataPoolRow next = poolEmpty.next();

		Assert.assertTrue(next==null || ((FileDataPoolImpl.ExtendedFile)next.getValue()).getName().equals(".gitignore"));
		
		poolEmpty.close();
	}
	
	@Test
	public void testDirectoryDataPoolTestToString() {
	Assert.assertTrue(pool.next().getValue().toString().endsWith("File.txt"));
	}
	
	@Test
	public void testDirectoryDataPoolTestGetName() {
		ExtendedFile value = (ExtendedFile) pool.next().getValue();
		Assert.assertEquals("File", value.getNameWithoutExtension());
		Assert.assertEquals("File.txt",(value).getName());
	}
	
	@Test
	public void testDirectoryDataPoolTestGetPath() {		
		Assert.assertEquals(file.getAbsolutePath(),((ExtendedFile)pool.next().getValue()).getPath());
	}
}
