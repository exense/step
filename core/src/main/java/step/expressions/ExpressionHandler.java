package step.expressions;

import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import step.commons.conf.Configuration;
import step.core.execution.ExecutionContext;

public class ExpressionHandler {
		
	private static Logger logger = LoggerFactory.getLogger(ExpressionHandler.class);
	
	private static final GroovyClassLoader groovyLoader = new GroovyClassLoader();
		
	private final CompilerConfiguration groovyCompilerConfiguration;
	
	public ExpressionHandler() {
		this("step.expressions.GroovyFunctions");
	}
	
	public ExpressionHandler(String baseScriptBase ) {
		super();
		
		groovyCompilerConfiguration = new CompilerConfiguration();
		if(baseScriptBase!=null) {
			groovyCompilerConfiguration.setScriptBaseClass(baseScriptBase);
		}
	}

	public String evaluateAttributeParameter(String initialValue) {
		String replacedValue = replace(initialValue);
		return replacedValue;
	}

	public Object evaluate(String expression) {
		Object result;
		try {			
			logger.debug("Groovy evaluation:\n" + expression);

			Binding binding = new Binding(); 
			
			ExecutionContext context = ExecutionContext.getCurrentContext();
			Map<String, Object> variableMap = context.getVariablesManager().getAllVariables();
			for(Entry<String, Object> varEntry : variableMap.entrySet()) {
				Object value =  varEntry.getValue();
				binding.setVariable(varEntry.getKey(), value);
			}

			long t1 = System.currentTimeMillis();	
			try {
				if(Configuration.getInstance().getPropertyAsBoolean("tec.expressions.usecache")) {
					GroovyPoolEntry entry = GroovyPool.getINSTANCE().borrowShell(expression);
					try {
						Script script = entry.getScript();
						script.setBinding(binding);
						result = script.run();
					} finally {
						if(entry!=null && entry.getScript()!=null) {
							// Release bindings to avoid references to be kept by the pool
							entry.getScript().setBinding(new Binding());							
						}
						GroovyPool.getINSTANCE().returnShell(entry);
					}
				} else {
					GroovyShell shell = new GroovyShell(groovyLoader, binding, groovyCompilerConfiguration);
					result = shell.evaluate(expression);
				}
			} catch (Exception e) {
				logger.error("An error occurred while evaluation groovy expression " + expression, e);
				throw e;
			}
			long duration = System.currentTimeMillis()-t1;
			
			int warnThreshold = Configuration.getInstance().getPropertyAsInteger("tec.expressions.warningthreshold");
			if(duration > warnThreshold) {
				logger.warn("Groovy-Evaluation of following expression took " + duration + ".ms: "+ expression);
			} else {
				logger.debug("Groovy-Evaluation of following expression took " + duration + ".ms: "+ expression);
			}
			
			logger.debug("Groovy Result:\n" + result);
			return result;
		} catch (CompilationFailedException cfe) {
			throw new RuntimeException(
					"Fehler im Groovyausdruck: " + expression, cfe);
		} catch (Exception e){
			throw new RuntimeException(
					"Fehler waehrend Evaluierung: " + expression, e);

		}
	}

	
	private String replace(String original) {
		// TODO implement
		return original;
	}
}
