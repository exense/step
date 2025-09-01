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
package step.expressions;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroovyPool implements AutoCloseable{
	
	private static final Logger logger = LoggerFactory.getLogger(GroovyPool.class);
		
	private GenericKeyedObjectPool<GroovyPoolKey, GroovyPoolEntry> pool;

	public GroovyPool(String scriptBaseClass, int poolMaxTotal, int poolMaxTotalPerKey, int poolMaxIdlePerKey) {
		this(new GroovyPoolFactory(scriptBaseClass), poolMaxTotal, poolMaxTotalPerKey, poolMaxIdlePerKey);
	}

	public GroovyPool(GroovyPoolFactory groovyPoolFactory, int poolMaxTotal, int poolMaxTotalPerKey, int poolMaxIdlePerKey) {
		super();
		
		try {
			pool = new GenericKeyedObjectPool<>(groovyPoolFactory);
			pool.setTestOnBorrow(true);
			pool.setMaxTotal(poolMaxTotal);
			pool.setMaxTotalPerKey(poolMaxTotalPerKey);
			pool.setMaxIdlePerKey(poolMaxIdlePerKey);
			pool.setBlockWhenExhausted(true);
			pool.setTimeBetweenEvictionRunsMillis(30000);
			pool.setMinEvictableIdleTimeMillis(-1);
		} catch(Exception e) {
			String errorMessage = "An error occurred while starting GroovyPool.";
			logger.error(errorMessage, e);
			throw new RuntimeException(errorMessage, e);
		}
	}

	public GroovyPoolEntry borrowShell(String script) throws Exception {
		GroovyPoolKey key = new GroovyPoolKey(script);
		GroovyPoolEntry entry;
		try {
			if (pool.getNumActive() == pool.getMaxTotal()) {
				logger.warn("The groovy pool is exhausted.");
			}
			if (pool.getNumActive(key) == pool.getMaxTotalPerKey()) {
				logger.warn("The groovy pool is exhausted for the expression {}.", script);
			}
			entry = pool.borrowObject(key);
			return entry;
		} catch (Exception e) {
			// Exceptions are thrown in case of invalid groovy expressions which is a
			// standard path. Thus logging in debug level only  
			if(logger.isDebugEnabled()) {
				logger.debug("An error occurred while borrowing script: " + script, e);
			}
			throw e;
		}
	}
	
	public void returnShell(GroovyPoolEntry entry) {
		try {
			pool.returnObject(entry.getKey(), entry);
		} catch (Exception e) {
			logger.warn("An error occurred while returning script: " + (String)((entry!=null&&entry.key!=null)?entry.key.getScript():"N/A"), e);
		}
	}



	@Override
	public void close() {
		pool.close();
	}
}
