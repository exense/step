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
	
	public GroovyPoolFactory() {
		super();
		//groovyCompilerConfiguration.setScriptBaseClass("step.expressions.GroovyFunctions");	
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
