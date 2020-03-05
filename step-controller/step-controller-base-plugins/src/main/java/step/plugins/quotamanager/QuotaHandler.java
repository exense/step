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

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.codehaus.groovy.runtime.InvokerHelper;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import step.plugins.quotamanager.config.Quota;

public class QuotaHandler {
	
	private ConcurrentHashMap<String, QuotaSemaphore> semaphores = new ConcurrentHashMap<>();
	
	private Quota config;
		
	private Class<?> scriptClass;
	
	public QuotaHandler(Quota config) {
		super();
		this.config = config;
		GroovyClassLoader groovyClassLoader = new GroovyClassLoader(); 
		try {
			scriptClass = groovyClassLoader.parseClass(config.getQuotaKeyFunction());
		} finally {
			try {
				groovyClassLoader.close();
			} catch (IOException e) {}
		}
	}

	public Quota getConfig() {
		return config;
	}
	
	public String acquirePermit(Map<String, Object> bindingVariables) throws Exception {
		String quotaKey = computeQuotaKey(bindingVariables);
		if(quotaKey!=null) {
			QuotaSemaphore semaphore = getOrCreateSemaphore(quotaKey);
			
			Long acquireTimeoutMs = config.getAcquireTimeoutMs();
			if(acquireTimeoutMs!=null) {
				boolean acquired;
				if(acquireTimeoutMs==0) {
					 semaphore.acquire();
					 acquired = true;
				} else {
					 acquired = semaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
				}
				if(!acquired) {
					throw new TimeoutException("A timeout occurred while trying to acquire permit for quota: " + config.toString());
				} else {
					semaphore.incrementLoad();
				}
			} else {
				semaphore.acquire();
				semaphore.incrementLoad();
			}
			
		}
		return quotaKey;
	}
	
	public String tryAcquirePermit(Map<String, Object> bindingVariables, long timeout) throws Exception {
		String quotaKey = computeQuotaKey(bindingVariables);
		if(quotaKey!=null) {
			QuotaSemaphore semaphore = getOrCreateSemaphore(quotaKey);
			
			boolean acquired = semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);				
			if(!acquired) {
				throw new TimeoutException("A timeout occurred while trying to acquire permit for quota: " + config.toString());
			} else {
				semaphore.incrementLoad();
			}
				
		}
		return quotaKey;
	}
	
	public void releasePermit(String quotaKey) {
		QuotaSemaphore semaphore = getOrCreateSemaphore(quotaKey);
		semaphore.decrementLoad();
		semaphore.release();
	}
	
	private QuotaSemaphore getOrCreateSemaphore(String key) {
		QuotaSemaphore semaphore = semaphores.get(key);
		if(semaphore == null) {
			QuotaSemaphore newInstance = new QuotaSemaphore(config.getPermits(), false);
			semaphore = semaphores.putIfAbsent(key, newInstance);
			if(semaphore == null) {
				semaphore = newInstance;
			}
		}
		return semaphore;
	}
	
	protected String computeQuotaKey(Map<String, Object> bindingVariables) throws Exception {
		Binding binding = new Binding(bindingVariables);
		Script script = InvokerHelper.createScript(scriptClass, binding);
		Object result = script.run();
		if(result!=null) {
			return result.toString();
		} else {
			return null;
		}
	}
	
	public QuotaHandlerStatus getStatus() {
		QuotaHandlerStatus status = new QuotaHandlerStatus();
		status.permitsByQuotaKey = config.getPermits();
		status.configuration = config;
		for(Entry<String, QuotaSemaphore> entry:semaphores.entrySet()) {
			int peak = entry.getValue().getPeak();
			int usage = config.getPermits()-entry.getValue().availablePermits();
			status.addEntry(entry.getKey(),usage, peak);
		}
		return status;
	}
	
}
