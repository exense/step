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
package step.plugins.quotamanager;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import junit.framework.Assert;
import step.commons.helpers.FileHelper;
import step.plugins.quotamanager.config.Quota;
import step.plugins.quotamanager.config.QuotaManagerConfig;

public class QuotaManagerTest {

	@Test
	public void testBasic() throws Exception {
		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		Quota quota = new Quota();
		quota.setPermits(1);
		quota.setQuotaKeyFunction("key");
		List<Quota> quotas = new ArrayList<>();
		quotas.add(quota);
		config.setQuotas(quotas);
		
		HashMap<String, Object> bindings = new HashMap<>();
		bindings.put("key", "key1");
		
		QuotaManager manager = new QuotaManager(config);
		UUID id = manager.acquirePermit(bindings);
		Assert.assertNotNull(id);
		manager.releasePermit(id);
	}
	
	@Test
	public void testMultipleQuota() throws Exception {
		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		List<Quota> quotas = new ArrayList<>();
		Quota quota = new Quota();
		quota.setPermits(1);
		quota.setQuotaKeyFunction("key");
		quotas.add(quota);
		
		quota = new Quota();
		quota.setPermits(1);
		quota.setQuotaKeyFunction("key");
		quotas.add(quota);
		config.setQuotas(quotas);
		
		HashMap<String, Object> bindings = new HashMap<>();
		bindings.put("key", "key1");
		
		QuotaManager manager = new QuotaManager(config);
		for(int i=0;i<10;i++) {
			UUID id = manager.acquirePermit(bindings);
			Assert.assertNotNull(id);
			manager.releasePermit(id);
		}
	}
	
	@Test
	public void testQuotaKeys() throws Exception {
		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		List<Quota> quotas = new ArrayList<>();
		Quota quota = new Quota();
		quota.setPermits(1);
		quota.setQuotaKeyFunction("key");
		quotas.add(quota);
		
		
		QuotaManager manager = new QuotaManager(config);
		for(int i=0;i<10;i++) {
			HashMap<String, Object> bindings = new HashMap<>();
			bindings.put("key", "key"+i);

			UUID id = manager.acquirePermit(bindings);
			Assert.assertNotNull(id);
		}
	}
	
	@Test
	public void testAcquireException() throws Exception {
		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		Quota quota = new Quota();
		quota.setPermits(1);
		quota.setAcquireTimeoutMs(0);
		quota.setQuotaKeyFunction("key");
		List<Quota> quotas = new ArrayList<>();
		quotas.add(quota);
		config.setQuotas(quotas);
		
		HashMap<String, Object> bindings = new HashMap<>();
		bindings.put("key", "key1");
		
		QuotaManager manager = new QuotaManager(config);
		boolean exceptionThrown = false;
		try {
			UUID id = manager.acquirePermit(bindings);
			Assert.assertNotNull(id);
			manager.acquirePermit(bindings);
			Assert.assertTrue(false);
		} catch (TimeoutException e) {
			exceptionThrown = true;
		}
		Assert.assertTrue(exceptionThrown);
		
	}
	
	@Test
	public void testNullQuotaKey() throws Exception {
		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		Quota quota = new Quota();
		quota.setPermits(1);
		quota.setQuotaKeyFunction("(key=='key1')?key:null");
		List<Quota> quotas = new ArrayList<>();
		quotas.add(quota);
		config.setQuotas(quotas);
		
		HashMap<String, Object> bindings = new HashMap<>();
		bindings.put("key", "key2");
		
		QuotaManager manager = new QuotaManager(config);
		for(int i=0;i<100;i++) {
			UUID id = manager.acquirePermit(bindings);
			Assert.assertNotNull(id);
			UUID id2 = manager.acquirePermit(bindings);
			Assert.assertNotNull(id2);
		}
	}
	
	@Test
	public void testParallel() throws Exception {
		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		Quota quota = new Quota();
		quota.setPermits(1);
		quota.setQuotaKeyFunction("(key=='key2')?key:null");
		List<Quota> quotas = new ArrayList<>();
		quotas.add(quota);
		config.setQuotas(quotas);
		
		final HashMap<String, Object> bindings = new HashMap<>();
		bindings.put("key", "key2");
		
		final QuotaManager manager = new QuotaManager(config);
		
		int n = 5;
		ExecutorService service = Executors.newFixedThreadPool(n);
		
		final Semaphore s = new Semaphore(1);
		
		final List<Exception> ex = new ArrayList<>();
		for(int j=0;j<n;j++) {
			service.submit(new Runnable() {
				public void run() {
					for(int i=0;i<100;i++) {
						UUID id;
						try {
							id = manager.acquirePermit(bindings);
							s.tryAcquire(0, TimeUnit.MILLISECONDS);
							Thread.sleep(1);
							s.release();
							Assert.assertNotNull(id);
							manager.releasePermit(id);
						} catch (Exception e) {
							ex.add(e);
						}
					}
				};
			});
		};
		
		service.shutdown();
		service.awaitTermination(1, TimeUnit.HOURS);
		
		if(ex.size()>0) {
			throw ex.get(0);
		}
	}
	
	@Test
	public void testParallel2() throws Exception {
		int n = 10;

		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		Quota quota = new Quota();
		quota.setPermits(n);
		quota.setAcquireTimeoutMs(0);
		quota.setQuotaKeyFunction("(key=='key2')?key:null");
		List<Quota> quotas = new ArrayList<>();
		quotas.add(quota);
		config.setQuotas(quotas);
		
		final HashMap<String, Object> bindings = new HashMap<>();
		bindings.put("key", "key2");
		
		final QuotaManager manager = new QuotaManager(config);
		
		ExecutorService service = Executors.newFixedThreadPool(n);
				
		long t1 = System.currentTimeMillis();
		
		final List<Exception> ex = new ArrayList<>();
		for(int j=0;j<n;j++) {
			service.submit(new Runnable() {
				public void run() {
					for(int i=0;i<5000;i++) {
						UUID id;
						try {
							id = manager.acquirePermit(bindings);
							Assert.assertNotNull(id);
							manager.releasePermit(id);
						} catch (Exception e) {
							ex.add(e);
						}
					}
				};
			});
		};
		service.shutdown();
		service.awaitTermination(1, TimeUnit.HOURS);

		long duration = System.currentTimeMillis() - t1;
		
		if(ex.size()>0) {
			throw ex.get(0);
		}
	}
	

	@Test
	public void testStatus() throws Exception {
		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		Quota quota = new Quota();
		quota.setPermits(1);
		quota.setQuotaKeyFunction("key");
		List<Quota> quotas = new ArrayList<>();
		quotas.add(quota);
		config.setQuotas(quotas);
		
		HashMap<String, Object> bindings = new HashMap<>();
		bindings.put("key", "key1");
		
		QuotaManager manager = new QuotaManager(config);
		UUID id = manager.acquirePermit(bindings);
		Assert.assertNotNull(id);
		
		List<QuotaHandlerStatus> status = manager.getStatus();
		Assert.assertEquals(1, status.size());
		Assert.assertEquals(1, status.get(0).getEntries().size());
		Assert.assertEquals(1, status.get(0).getEntries().get(0).getUsage());
		Assert.assertEquals(1, status.get(0).getEntries().get(0).getPeak());
		manager.releasePermit(id);
		
		status = manager.getStatus();
		Assert.assertEquals(1, status.size());
		Assert.assertEquals(1, status.get(0).getEntries().size());
		Assert.assertEquals(0, status.get(0).getEntries().get(0).getUsage());
		Assert.assertEquals(1, status.get(0).getEntries().get(0).getPeak());
	}
	
	@Test
	public void testConfigParser() throws Exception {
		QuotaManagerConfig config = new QuotaManagerConfig("testManager");
		Quota quota = new Quota();
		quota.setPermits(1);
		quota.setQuotaKeyFunction("key");
		List<Quota> quotas = new ArrayList<>();
		quotas.add(quota);
		config.setQuotas(quotas);
		
		HashMap<String, Object> bindings = new HashMap<>();
		bindings.put("key", "key1");
		bindings.put("node", "foo");
		
		QuotaManager manager = new QuotaManager(FileHelper.getClassLoaderResource(this.getClass(),"QuotaManagerConfig.xml"));
		UUID id = manager.acquirePermit(bindings);
		Assert.assertNotNull(id);
		manager.releasePermit(id);
	}
	
}
