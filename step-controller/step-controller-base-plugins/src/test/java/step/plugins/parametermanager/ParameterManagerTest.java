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

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Test;

import step.parameter.Parameter;
import step.commons.activation.Expression;
import ch.exense.commons.core.accessors.InMemoryAccessor;
import step.core.objectenricher.ObjectPredicate;
import step.parameter.ParameterManager;

public class ParameterManagerTest {

	@Test
	public void testJavascript() throws ScriptException {
		Configuration configuration = new Configuration();
		configuration.putProperty("tec.activator.scriptEngine","javascript");
		test1Common(configuration);
	}

	@Test
	public void testGroovy() throws ScriptException {
		Configuration configuration = new Configuration();
		configuration.putProperty("tec.activator.scriptEngine","groovy");
		test1Common(configuration);
	}

	public void test1Common(Configuration configuration) throws ScriptException {
		InMemoryAccessor<Parameter> accessor = new InMemoryAccessor<>();
		ParameterManager m = new ParameterManager(accessor, null, configuration);

		accessor.save(new Parameter(new Expression("user=='pomme'"), "key1", "pommier", "desc"));
		accessor.save(new Parameter(new Expression("user=='pomme'"), "key1", "pommier", "desc"));
		accessor.save(new Parameter(new Expression("user=='abricot'"), "key1", "abricotier", "desc"));
		accessor.save(new Parameter(new Expression("user=='poire'"), "key1", "poirier", "desc"));

		accessor.save(new Parameter(null, "key2", "defaultValue", "desc"));
		accessor.save(new Parameter(null, "key2", "defaultValue2", "desc"));
		accessor.save(new Parameter(new Expression("user=='poire'"), "key2", "defaultValue2", "desc"));

		accessor.save(new Parameter(null, "key3", "value1", "desc"));
		accessor.save(new Parameter(new Expression("user=='poire'"), "key3", "value2", "desc"));
		Parameter p = new Parameter(new Expression("user=='poire'"), "key3", "value3", "desc");
		p.setPriority(10);
		accessor.save(p);

		Map<String, Object> bindings = new HashMap<String, Object>();
		bindings.put("user", "poire");

		Map<String, String> params = m.getAllParameterValues(bindings, null);
		Assert.assertEquals(params.get("key1"),"poirier");
		Assert.assertEquals(params.get("key2"),"defaultValue2");
		Assert.assertEquals(params.get("key3"),"value3");

		params = m.getAllParameterValues(bindings, new ObjectPredicate() {
			@Override
			public boolean test(Object t) {
				return false;
			}
		});
		Assert.assertEquals(0, params.size());
	}
	
	@Test
	public void testPerf() throws ScriptException {
		InMemoryAccessor<Parameter> accessor = new InMemoryAccessor<>();
		ParameterManager m = new ParameterManager(accessor, null, new Configuration());
		
		int nIt = 100;
		for(int i=1;i<=nIt;i++) {
			accessor.save(new Parameter(new Expression("user=='user"+i+"'"), "key1", "value"+i, "desc"));
		}
		
		Map<String, Object> bindings = new HashMap<String, Object>();
		bindings.put("user", "user"+nIt);
		
		long t1 = System.currentTimeMillis();
		Map<String, String> params = m.getAllParameterValues(bindings, null);
		System.out.println("ms:"+(System.currentTimeMillis()-t1));
		Assert.assertEquals(params.get("key1"),"value"+nIt);
		
		t1 = System.currentTimeMillis();
		params = m.getAllParameterValues(bindings, null);
		System.out.println("ms:"+(System.currentTimeMillis()-t1));
		Assert.assertEquals(params.get("key1"),"value"+nIt);
		
		Assert.assertTrue((System.currentTimeMillis()-t1)<500);	
	}
	
	@Test
	public void testParallel() throws ScriptException, InterruptedException, ExecutionException {
		InMemoryAccessor<Parameter> accessor = new InMemoryAccessor<>();
		ParameterManager m = new ParameterManager(accessor, null, new Configuration());
		
		int nIt = 100;
		for(int i=1;i<=nIt;i++) {
			accessor.save(new Parameter(new Expression("user=='user"+i+"'"), "key1", "value"+i, "desc"));
		}
		
		int iterations = 25;
		
		int nThreads = 4;
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
						Map<String, String> params = m.getAllParameterValues(bindings, null);
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
				throw e1;
			}
		}
	}
}
