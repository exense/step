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

import step.commons.conf.Configuration;

public class GroovyPool {
	
	private static final Logger logger = LoggerFactory.getLogger(GroovyPool.class);
	
	private static GroovyPool INSTANCE = new GroovyPool();
	
	private GenericKeyedObjectPool<GroovyPoolKey, GroovyPoolEntry> pool;

	public static GroovyPool getINSTANCE() {
		return INSTANCE;
	}

	private GroovyPool() {
		super();
		
		try {
			pool = new GenericKeyedObjectPool<>(new GroovyPoolFactory());
			pool.setTestOnBorrow(true);
			pool.setMaxTotal(Configuration.getInstance().getPropertyAsInteger("tec.expressions.pool.maxtotal",1000));
			pool.setMaxActive(-1);
			pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK);
			pool.setTimeBetweenEvictionRunsMillis(30000);
			pool.setMinEvictableIdleTimeMillis(-1);
		} catch(Exception e) {
			logger.error("An error occurred while starting GroovyPool.", e);
		}
	}

	public static void setINSTANCE(GroovyPool iNSTANCE) {
		INSTANCE = iNSTANCE;
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
			e.printStackTrace();
		}
	}
	
//	public static void main(String[] args) {
//		long t1 = System.currentTimeMillis();
//		GroovyPoolEntry entry;
//		for(int i=0;i<1000;i++) {
//		try {
//			entry = GroovyPool.getINSTANCE().borrowShell("Meldung == 'erfolgreich'");
//			
//			try {
//				Script script = entry.getScript();
//				Binding b = new Binding();
//				b.setVariable("Meldung", "erfolgreich");
//				script.setBinding(b);
//				Object result = script.run();
////				System.out.println(result);		
//			} finally {
//				GroovyPool.getINSTANCE().returnShell(entry);
//			}
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		}
//		System.out.println((long)(System.currentTimeMillis()-t1));
//	}
	
}
