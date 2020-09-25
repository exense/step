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

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroovyPoolFactory implements KeyedPoolableObjectFactory<GroovyPoolKey, GroovyPoolEntry>{
	
	private static final Logger logger = LoggerFactory.getLogger(GroovyPoolFactory.class);
	
	private CompilerConfiguration groovyCompilerConfiguration = new CompilerConfiguration();
	
	public GroovyPoolFactory(String scriptBaseClass) {
		super();
		if(scriptBaseClass!=null) {
			groovyCompilerConfiguration.setScriptBaseClass(scriptBaseClass);				
		}
	}

	@Override
	public void activateObject(GroovyPoolKey arg0, GroovyPoolEntry arg1)
			throws Exception {}

	@Override
	public void destroyObject(GroovyPoolKey arg0, GroovyPoolEntry arg1)
			throws Exception {	}

	@Override
	public GroovyPoolEntry makeObject(GroovyPoolKey arg0) throws Exception {
		logger.debug("Creating new script: " + arg0.getScript());
		GroovyShell shell = new GroovyShell(groovyCompilerConfiguration);
		Script script = shell.parse(arg0.getScript());
		
		GroovyPoolEntry result = new GroovyPoolEntry(arg0, script);
		return result;
	}

	@Override
	public void passivateObject(GroovyPoolKey arg0, GroovyPoolEntry arg1)
			throws Exception {}

	@Override
	public boolean validateObject(GroovyPoolKey arg0, GroovyPoolEntry arg1) {
		return true;
	}

}
