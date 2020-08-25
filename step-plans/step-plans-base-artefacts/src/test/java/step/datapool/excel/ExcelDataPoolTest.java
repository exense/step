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
package step.datapool.excel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.AbstractArtefactTest;
import step.core.dynamicbeans.DynamicValue;
import step.core.variables.SimpleStringMap;
import step.datapool.DataPoolRow;
import step.datapool.Utils;

public class ExcelDataPoolTest extends AbstractArtefactTest {

	@Test
	public void testDefaultSheet() {		
		ExcelDataPool conf = getDataSourceConf(false, "ExcelDataPool.xlsx", null);
		
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(conf);
		initPool(pool);
		
		Assert.assertEquals("DefaultValue", ((SimpleStringMap)pool.next().getValue()).get("A"));
		pool.close();
	}

	private void initPool(ExcelDataPoolImpl pool) {
		pool.setContext(newExecutionContext());
		pool.init();
	}

	@Test
	public void testToString() {		
		ExcelDataPool conf = getDataSourceConf(true, "ExcelDataPool.xlsx", "Parallel");
		
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(conf);
		
		initPool(pool);
		Assert.assertTrue(((SimpleStringMap)pool.next().getValue()).toString().startsWith("Keys=Key1 Values=Value1 Result="));
		pool.close();
	}
	
	@Test
	public void testReset() {		
		ExcelDataPool conf = getDataSourceConf(false, "ExcelDataPool.xlsx", null);
		
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(conf);
		
		initPool(pool);
		Assert.assertEquals("DefaultValue", ((SimpleStringMap)pool.next().getValue()).get("A"));
		
		pool.reset();
		Assert.assertEquals("DefaultValue", ((SimpleStringMap)pool.next().getValue()).get("A"));
		
		Assert.assertEquals(null, pool.next());
		
		pool.close();
	}
	
	@Test
	public void testParallel() throws InterruptedException {
		int nIt = 1000;
		int nThreads = 10;
		ExecutorService service = Executors.newFixedThreadPool(nThreads);
		
		ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
				
		ExcelDataPool conf = getDataSourceConf(true, "ExcelDataPool.xlsx", "Parallel");
		
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(conf);
		
		initPool(pool);

		List<Exception> exceptions = new ArrayList<>();
		
		
		String uid = UUID.randomUUID().toString();
		
		for(int j=0;j<nThreads;j++) {
			service.execute(new Runnable() {
				@Override
				public void run() {
					for(int i=0;i<nIt/nThreads;i++) {
						try {
							SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
							row.put("Result", uid+ "_" + row.get("Values"));
							map.put(row.get("Keys"), row.get("Values"));
							
							
						} catch(Exception e) {
							synchronized (e) {
								exceptions.add(e);
							}
							e.printStackTrace();
						}
					}
				}
			});
		}
		
		
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);
		
		for(int i=1;i<=nIt;i++) {
			Assert.assertEquals("Value"+i, map.get("Key"+i));
		}
		
		pool.save();
		
		pool.close();
		
		Assert.assertEquals(0, exceptions.size());

		ExcelDataPoolImpl pool2 = new ExcelDataPoolImpl(conf);
		
		initPool(pool2);
		
		for(int i=1;i<=nIt;i++) {
			SimpleStringMap row = (SimpleStringMap)pool2.next().getValue();
			Assert.assertEquals(uid+ "_Value"+i, row.get("Result"));
		}
		
		pool2.close();
	}

	private ExcelDataPool getDataSourceConf(boolean headers, String file, String worksheet) {
		ExcelDataPool conf = new ExcelDataPool();
		conf.setFile(new DynamicValue<String>(ExcelFunctionsTest.getResourceFile(file).getAbsolutePath()));
		conf.setHeaders(new DynamicValue<Boolean>(headers));
		conf.setWorksheet(new DynamicValue<String>(worksheet));
		return conf;
	}
	
	@Test
	public void testWithoutHeaders() {		
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(getDataSourceConf(false, "ExcelDataPool.xlsx", "WithoutHeaders"));
		
		initPool(pool);
		
		for(int i=1;i<=10;i++) {
			Assert.assertEquals("Value"+i, ((SimpleStringMap)pool.next().getValue()).get("B"));
		}
		
		Assert.assertNull(pool.next());
		
		pool.close();
	}
	
	@Test
	public void testCrossSheet() {	
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(getDataSourceConf(false, "ExcelDataPool.xlsx", "WithoutHeaders"));
		
		initPool(pool);
		
		for(int i=1;i<=10;i++) {
			Assert.assertEquals("Value"+i, ((SimpleStringMap)pool.next().getValue()).get("WithoutHeaders2::B"));
		}
		
		Assert.assertNull(pool.next());
		
		pool.close();
	}
	
	
	@Test
	public void testWithHeaders() {		
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(getDataSourceConf(true, "ExcelDataPool.xlsx", "WithHeaders"));
		
		initPool(pool);
		
		for(int i=1;i<=10;i++) {
			SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
			Assert.assertEquals("Value"+i, row.get("Values"));
			Assert.assertEquals("Key"+i, row.get("Keys"));
		
			// Test the Utils class at the same time
			JsonObject jsonRow = Utils.toJson(row);
			Assert.assertEquals("Key"+i, jsonRow.getString("Keys"));
			Assert.assertEquals("Value"+i, jsonRow.getString("Values"));
		}
		
		Assert.assertNull(pool.next());
		
		pool.close();
	}
	
	@Test
	public void testWrite() {
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(getDataSourceConf(true, "ExcelDataPool.xlsx", "Write"));

		initPool(pool);
		
		String value = UUID.randomUUID().toString();
		
		for(int i=1;i<=10;i++) {
			SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
			Assert.assertEquals("input"+i, row.get("Input"));
			row.put("Output", value+i);
		}
		
		pool.save();
		
		pool.close();

		pool = new ExcelDataPoolImpl(getDataSourceConf(true, "ExcelDataPool.xlsx", "Write"));
		
		initPool(pool);
		
		for(int i=1;i<=10;i++) {
			SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
			Assert.assertEquals("input"+i, row.get("Input"));
			Assert.assertEquals(value+i, row.get("Output"));
		}
		
		Assert.assertNull(pool.next());
		
		pool.close();
	}
	
	@Test
	public void testValueChanged() {
		ExcelDataPoolImpl pool  = new ExcelDataPoolImpl(getDataSourceConf(true, "ExcelDataPoolValueChanged.xlsx", "DefaultSheet"));

		initPool(pool);
				
		DataPoolRow r;
		while((r=pool.next())!=null) {
			SimpleStringMap row = (SimpleStringMap) r.getValue();
			Assert.assertEquals("ELSE", row.get("Value"));
			row.put("Actual", "passed");
			Assert.assertEquals("SKIP", row.get("Value"));
		}
		
		pool.close();
	}
	
	@Test
	public void testNoSave() {
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(getDataSourceConf(true, "ExcelDataPoolImmutable.xlsx", "Write"));
		
		initPool(pool);
		
		String value = UUID.randomUUID().toString();
		
		for(int i=1;i<=10;i++) {
			SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
			Assert.assertEquals("input"+i, row.get("Input"));
			row.put("Output", value+i);
		}
				
		pool.close();
		
		pool = new ExcelDataPoolImpl(getDataSourceConf(true, "ExcelDataPoolImmutable.xlsx", "Write"));
		
		initPool(pool);
				
		for(int i=1;i<=10;i++) {
			SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
			Assert.assertEquals("input"+i, row.get("Input"));
		}
		
		pool.close();
	}
	
	@Test
	public void testSKIP() {
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(getDataSourceConf(false, "ExcelDataPool.xlsx", "SKIP"));
		
		initPool(pool);
		
		int i=0;
		while(pool.next()!=null) {i++;}

		Assert.assertEquals(7,i);
		
		pool.close();
	}
	
	@Test
	public void testStop() {
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(getDataSourceConf(false, "ExcelDataPool.xlsx", "Stop"));
		
		initPool(pool);
		
		int i=0;
		while(pool.next()!=null) {i++;}

		Assert.assertEquals(3,i);
		
		pool.close();
	}
	
	@Test
	public void testAddRow() {
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(getDataSourceConf(true, "ExcelDataPool.xlsx", "AddRow"));
		initPool(pool);
		String uid = UUID.randomUUID().toString();
		
		HashMap<String, String> row = new HashMap<>();
		row.put("Col1", uid);
		row.put("Col2", uid);
		// test the autocreation of headers:
		row.put("Col"+uid, uid);
		pool.addRow(row);
		
		pool.save();
		
		pool.close();
		
		pool = new ExcelDataPoolImpl(getDataSourceConf(true, "ExcelDataPool.xlsx", "AddRow"));
		initPool(pool);
		
		try {
			boolean containsNewRow = false;
			boolean containsNewColumn = false;

			DataPoolRow next;
			while((next = pool.next())!=null) {
				SimpleStringMap map = (SimpleStringMap) next.getValue();
				if(map.get("Col1").equals(uid)) {
					containsNewRow = true;
				}
				if(map.get("Col"+uid).equals(uid)) {
					containsNewColumn = true;
				}
			}
			
			Assert.assertTrue(containsNewRow);
			Assert.assertTrue(containsNewColumn);
		} finally {
			pool.close();			
		}
	}
	
	@Test
	public void testAddRowToNewExcel() {
		String uid = UUID.randomUUID().toString();
		ExcelDataPool conf = new ExcelDataPool();
		conf.setFile(new DynamicValue<String>(ExcelFunctionsTest.getResourceFile(".").getAbsolutePath()+"/testNewExcel"+uid+".xlsx"));
		conf.setHeaders(new DynamicValue<Boolean>(true));
		conf.setWorksheet(new DynamicValue<String>("Test"));
		conf.setForWrite(new DynamicValue<Boolean>(true));
		
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(conf);
		initPool(pool);
		
		HashMap<String, String> row = new HashMap<>();
		row.put("Col1", uid);
		row.put("Col2", uid);
		// test the autocreation of headers:
		row.put("Col"+uid, uid);
		pool.addRow(row);
		
		pool.save();
		
		pool.close();
		
		pool = new ExcelDataPoolImpl(conf);
		initPool(pool);
		
		try {
			boolean containsNewRow = false;
			boolean containsNewColumn = false;

			DataPoolRow next;
			while((next = pool.next())!=null) {
				SimpleStringMap map = (SimpleStringMap) next.getValue();
				if(map.get("Col1").equals(uid)) {
					containsNewRow = true;
				}
				if(map.get("Col"+uid).equals(uid)) {
					containsNewColumn = true;
				}
			}
			
			Assert.assertTrue(containsNewRow);
			Assert.assertTrue(containsNewColumn);
		} finally {
			pool.close();			
		}
		
	}
}
