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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class GroovyPool implements AutoCloseable{
	
	private static final Logger logger = LoggerFactory.getLogger(GroovyPool.class);
		
	private GenericKeyedObjectPool<GroovyPoolKey, GroovyPoolEntry> pool;
	private ScheduledExecutorService scheduler = null;

	public GroovyPool(String scriptBaseClass, int poolMaxTotal, int poolMaxTotalPerKey, int poolMaxIdlePerKey, Integer monitoringIntervalSeconds) {
		this(new GroovyPoolFactory(scriptBaseClass), poolMaxTotal, poolMaxTotalPerKey, poolMaxIdlePerKey, monitoringIntervalSeconds);
	}

	public GroovyPool(GroovyPoolFactory groovyPoolFactory, int poolMaxTotal, int poolMaxTotalPerKey, int poolMaxIdlePerKey, Integer monitoringIntervalSeconds) {
		super();
		
		try {
			pool = new GenericKeyedObjectPool<>(groovyPoolFactory);
			pool.setTestOnBorrow(true);
			pool.setMaxTotal(poolMaxTotal);
			pool.setMaxTotalPerKey(poolMaxTotalPerKey);
			pool.setMaxIdlePerKey(poolMaxIdlePerKey);
			pool.setBlockWhenExhausted(true);
			pool.setTimeBetweenEvictionRuns(Duration.ofMillis(30000));;
			pool.setMinEvictableIdle(Duration.ofMillis(-1));
			if (monitoringIntervalSeconds != null) {
				ThreadFactory threadFactory = new ThreadFactoryBuilder()
						.setNameFormat("groovy-pool-monitor-%d")
						.setDaemon(true)
						.build();
				scheduler = Executors.newScheduledThreadPool(1, threadFactory);
				scheduler.scheduleAtFixedRate(this::checkPoolHealth, monitoringIntervalSeconds, monitoringIntervalSeconds, TimeUnit.SECONDS);
			}
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


	private void checkPoolHealth() {
		try {
			Map<String, Integer> numActivePerKey = pool.getNumActivePerKey();
			int active = numActivePerKey.values().stream().mapToInt(Integer::intValue).sum();
			int maxTotal = pool.getMaxTotal();

			if (active >= maxTotal * 0.9) { // 90% threshold
				logger.warn("Groovy pool capacity at risk: {}/{} active objects. You could increase the total pool size in the step.properties (i.e. tec.expressions.pool.maxtotal=" + maxTotal*2 + ")"
						, active, maxTotal);
			}
			checkHotKeys(numActivePerKey);
		} catch (Exception e) {
			logger.debug("Error monitoring pool", e);
		}
	}

	private void checkHotKeys(Map<String, Integer> numActivePerKey ) {
		Map<String, Integer> numWaitersByKey = pool.getNumWaitersByKey();
		int maxPerKey = pool.getMaxTotalPerKey();
		numActivePerKey.forEach((key,activePerKey) -> {
			if (activePerKey >= maxPerKey * 0.9) {
				logger.warn("Groovy pool capacity at risk for expression '{}': {}/{} active. You could increase the pool size per expression in the step.properties (i.e. tec.expressions.pool.maxTotalPerKey=" + maxPerKey*2 + ")",
						key, activePerKey, maxPerKey);
			}
		});
		numWaitersByKey.forEach((key, waitersPerKey) -> {
			if (waitersPerKey > 0) {
				logger.warn("Waiters to borrow an element from the Pool for expression '{}': {}/{} active, waiters: {}",
						key, numActivePerKey.get(key), maxPerKey, waitersPerKey);
			}
		});
	}


	@Override
	public void close() {
		pool.close();
		if (scheduler != null) {
			scheduler.shutdown();
		}
	}
}
