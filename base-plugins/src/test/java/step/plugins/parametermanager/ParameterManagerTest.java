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
package step.plugins.parametermanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;

import step.commons.activation.Expression;
import step.plugins.parametermanager.Parameter;
import step.plugins.parametermanager.ParameterManager;

public class ParameterManagerTest {

	@Test
	public void test1() throws ScriptException {
		ParameterManager m = new ParameterManager();
		
		m.addParameter(new Parameter(new Expression("user=='pomme'"), "key1", "pommier"));
		m.addParameter(new Parameter(new Expression("user=='abricot'"), "key1", "abricotier"));
		m.addParameter(new Parameter(new Expression("user=='poire'"), "key1", "poirier"));
		
		m.addParameter(new Parameter(null, "key2", "defaultValue"));
		m.addParameter(new Parameter(null, "key2", "defaultValue2"));
		m.addParameter(new Parameter(new Expression("user=='poire'"), "key2", "defaultValue2"));

		m.addParameter(new Parameter(null, "key3", "value1"));
		m.addParameter(new Parameter(new Expression("user=='poire'"), "key3", "value2"));
		Parameter p = new Parameter(new Expression("user=='poire'"), "key3", "value3");
		p.setPriority(10);
		m.addParameter(p);
		
		Map<String, Object> bindings = new HashMap<String, Object>();
		bindings.put("user", "poire");
				
		Map<String, String> params = m.getAllParameters(bindings);
		Assert.assertEquals(params.get("key1"),"poirier");
		Assert.assertEquals(params.get("key2"),"defaultValue2");
		Assert.assertEquals(params.get("key3"),"value3");
	}
	
	@Test
	public void testPerf() throws ScriptException {
		ParameterManager m = new ParameterManager();
		
		int nIt = 100;
		for(int i=1;i<=nIt;i++) {
			m.addParameter(new Parameter(new Expression("user=='user"+i+"'"), "key1", "value"+i));
		}
		
		Map<String, Object> bindings = new HashMap<String, Object>();
		bindings.put("user", "user"+nIt);
		
		long t1 = System.currentTimeMillis();
		Map<String, String> params = m.getAllParameters(bindings);
		System.out.println("ms:"+(System.currentTimeMillis()-t1));
		Assert.assertEquals(params.get("key1"),"value"+nIt);
		
		t1 = System.currentTimeMillis();
		params = m.getAllParameters(bindings);
		System.out.println("ms:"+(System.currentTimeMillis()-t1));
		Assert.assertEquals(params.get("key1"),"value"+nIt);
		
		Assert.assertTrue((System.currentTimeMillis()-t1)<500);	
	}
	
	@Test
	public void testParallel() throws ScriptException, InterruptedException, ExecutionException {
		ParameterManager m = new ParameterManager();
		
		int nIt = 100;
		for(int i=1;i<=nIt;i++) {
			m.addParameter(new Parameter(new Expression("user=='user"+i+"'"), "key1", "value"+i));
		}
		
		int iterations = 100;
		
		int nThreads = 10;
		ExecutorService e = Executors.newFixedThreadPool(10);
		List<Future> futures = new ArrayList<>();
		for(int j=0; j<nThreads;j++) {
			futures.add(e.submit(new Runnable() {
				@Override
				public void run() {
					for(int i=0;i<iterations;i++) {
						Map<String, Object> bindings = new HashMap<String, Object>();
						Random r = new Random();
						int userId = r.nextInt(nIt)+1;
						bindings.put("user", "user"+userId);
						Map<String, String> params = m.getAllParameters(bindings);
						Assert.assertEquals(params.get("key1"),"value"+userId);
					}
				}
			}));
		}
			
		e.shutdown();
		e.awaitTermination(1, TimeUnit.MINUTES);
		
		for(Future f:futures) {
			try {
				f.get();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				throw e1;
			}
		}
	}
}
