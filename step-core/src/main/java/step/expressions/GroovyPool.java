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
package step.expressions;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroovyPool {
	
	private static final Logger logger = LoggerFactory.getLogger(GroovyPool.class);
		
	private GenericKeyedObjectPool<GroovyPoolKey, GroovyPoolEntry> pool;

	public GroovyPool(String scriptBaseClass, int poolMaxTotal, int poolMaxIdle) {
		super();
		
		try {
			pool = new GenericKeyedObjectPool<>(new GroovyPoolFactory(scriptBaseClass));
			pool.setTestOnBorrow(true);
			pool.setMaxTotal(poolMaxTotal);
			pool.setMaxActive(-1);
			pool.setMaxIdle(poolMaxIdle);
			pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK);
			pool.setTimeBetweenEvictionRunsMillis(30000);
			pool.setMinEvictableIdleTimeMillis(-1);
		} catch(Exception e) {
			logger.error("An error occurred while starting GroovyPool.", e);
		}
	}

	public GroovyPoolEntry borrowShell(String script) throws Exception {
		GroovyPoolKey key = new GroovyPoolKey(script);
		GroovyPoolEntry entry;
		try {
			entry = pool.borrowObject(key);
			return entry;
		} catch (Exception e) {
			logger.error("An error occurred while borrowing script: " + script, e);
			throw e;
		}
	}
	
	public void returnShell(GroovyPoolEntry entry) {
		try {
			pool.returnObject(entry.getKey(), entry);
		} catch (Exception e) {
			logger.error("An error occurred while returning script: " + (String)((entry!=null&&entry.key!=null)?entry.key.getScript():"N/A"), e);
		}
	}
}
