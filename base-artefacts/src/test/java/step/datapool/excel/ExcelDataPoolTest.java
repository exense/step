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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.ForEachBlock;
import step.core.variables.SimpleStringMap;
import step.datapool.DataPoolRow;
import step.datapool.Utils;

public class ExcelDataPoolTest {

	@Test
	public void testDefaultSheet() {
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.FALSE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPool.xlsx").getAbsolutePath());
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		Assert.assertEquals("DefaultValue", ((SimpleStringMap)pool.next().getValue()).get("A"));
		pool.close();
	}
	
	@Test
	public void testParallel() throws InterruptedException {
		int nIt = 1000;
		int nThreads = 10;
		ExecutorService service = Executors.newFixedThreadPool(nThreads);
		
		ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
		
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.TRUE.toString());
		
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPool.xlsx").getAbsolutePath()+"::Parallel");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();

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

		ExcelDataPoolImpl pool2 = new ExcelDataPoolImpl(artefact);
		
		pool2.reset();
		
		for(int i=1;i<=nIt;i++) {
			SimpleStringMap row = (SimpleStringMap)pool2.next().getValue();
			Assert.assertEquals(uid+ "_Value"+i, row.get("Result"));
		}
		
		pool2.close();
	}
	
	@Test
	public void testWithoutHeaders() {
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.FALSE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPool.xlsx").getAbsolutePath()+"::WithoutHeaders");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		
		for(int i=1;i<=10;i++) {
			Assert.assertEquals("Value"+i, ((SimpleStringMap)pool.next().getValue()).get("B"));
		}
		
		Assert.assertNull(pool.next());
		
		pool.close();
	}
	
	@Test
	public void testCrossSheet() {
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.FALSE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPool.xlsx").getAbsolutePath()+"::WithoutHeaders");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		
		for(int i=1;i<=10;i++) {
			Assert.assertEquals("Value"+i, ((SimpleStringMap)pool.next().getValue()).get("WithoutHeaders2::B"));
		}
		
		Assert.assertNull(pool.next());
		
		pool.close();
	}
	
	
	@Test
	public void testWithHeaders() {
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.TRUE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPool.xlsx").getAbsolutePath()+"::WithHeaders");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		
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
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.TRUE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPool.xlsx").getAbsolutePath()+"::Write");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		
		String value = UUID.randomUUID().toString();
		
		for(int i=1;i<=10;i++) {
			SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
			Assert.assertEquals("input"+i, row.get("Input"));
			row.put("Output", value+i);
		}
		
		pool.save();
		
		pool.close();

		pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		
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
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.TRUE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPoolValueChanged.xlsx").getAbsolutePath()+"::DefaultSheet");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
				
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
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.TRUE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPoolImmutable.xlsx").getAbsolutePath()+"::Write");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		
		String value = UUID.randomUUID().toString();
		
		for(int i=1;i<=10;i++) {
			SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
			Assert.assertEquals("input"+i, row.get("Input"));
			row.put("Output", value+i);
		}
				
		pool.close();
		
		pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
				
		for(int i=1;i<=10;i++) {
			SimpleStringMap row = (SimpleStringMap)pool.next().getValue();
			Assert.assertEquals("input"+i, row.get("Input"));
		}
		
		pool.close();
	}
	
	@Test
	public void testSKIP() {
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.FALSE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPool.xlsx").getAbsolutePath()+"::SKIP");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		
		int i=0;
		while(pool.next()!=null) {i++;}

		Assert.assertEquals(7,i);
		
		pool.close();
	}
	
	@Test
	public void testStop() {
		ForEachBlock artefact = new ForEachBlock();
		
		artefact.setHeader(Boolean.FALSE.toString());
		artefact.setTable(ExcelFunctionsTest.getResourceFile("ExcelDataPool.xlsx").getAbsolutePath()+"::Stop");
		ExcelDataPoolImpl pool = new ExcelDataPoolImpl(artefact);
		
		pool.reset();
		
		int i=0;
		while(pool.next()!=null) {i++;}

		Assert.assertEquals(3,i);
		
		pool.close();
	}
}
