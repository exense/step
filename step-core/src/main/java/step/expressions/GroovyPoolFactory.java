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

import groovy.lang.GroovyShell;
import groovy.lang.Script;


import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroovyPoolFactory implements KeyedPooledObjectFactory<GroovyPoolKey, GroovyPoolEntry> {
	
	private static final Logger logger = LoggerFactory.getLogger(GroovyPoolFactory.class);
	
	private CompilerConfiguration groovyCompilerConfiguration = new CompilerConfiguration();


	public GroovyPoolFactory(String scriptBaseClass) {
		super();
		if(scriptBaseClass!=null) {
			groovyCompilerConfiguration.setScriptBaseClass(scriptBaseClass);
		}
	}

	@Override
	public PooledObject<GroovyPoolEntry> makeObject(GroovyPoolKey groovyPoolKey) throws Exception {
		logger.debug("Creating new script: " + groovyPoolKey.getScript());
		GroovyShell shell = new GroovyShell(groovyCompilerConfiguration);
		Script script = shell.parse(groovyPoolKey.getScript());

		GroovyPoolEntry result = new GroovyPoolEntry(groovyPoolKey, script);
		return new DefaultPooledObject<>(result);
	}

	@Override
	public void activateObject(GroovyPoolKey groovyPoolKey, PooledObject<GroovyPoolEntry> pooledObject) throws Exception {

	}

	@Override
	public void destroyObject(GroovyPoolKey groovyPoolKey, PooledObject<GroovyPoolEntry> pooledObject) throws Exception {
	}

	@Override
	public void passivateObject(GroovyPoolKey groovyPoolKey, PooledObject<GroovyPoolEntry> pooledObject) throws Exception {
	}

	@Override
	public boolean validateObject(GroovyPoolKey groovyPoolKey, PooledObject<GroovyPoolEntry> pooledObject) {
		return true;
	}
}
