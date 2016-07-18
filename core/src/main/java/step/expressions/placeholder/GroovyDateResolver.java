package step.expressions.placeholder;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.codehaus.groovy.control.CompilationFailedException;

public class GroovyDateResolver implements Resolver {

	public static final String RPL_MARK = "//_TimeCategoryExpression_//";
	
//	private Pattern groovyDatePattern = Pattern.compile("^[ ]*(today|last|now|first)[ ]*[+-].*$");
//	
//	private Matcher groovyDateMatcher = groovyDatePattern.matcher("");
	
	private static GroovyScriptEngine scriptEngine;
	
	private static GroovyObject datePlaceholderScript;
	
	private static String timeCategoryExpressionScript;

	public GroovyDateResolver() {
		super();
		loadScriptEngine();
		loadGroovyDatePlaceholder();
		loadGroovyTimeCategoryScript();
	}

	@Override
	public String resolve(String name) {
		String result = resolveDatePlaceholder(name);
		if(result == null) {
			result = resolveTimeCategoryExpression(name);
		}
		return result;
	}
	
	private String resolveDatePlaceholder(String name) {
		// Using the script engine as follow to run the script is less efficient:
		// String result = scriptEngine.run("DatePlaceholders.groovy", name);
		return (String) datePlaceholderScript.invokeMethod("resolve", name);
	}
	
	private String resolveTimeCategoryExpression(String expression) {
		String script = timeCategoryExpressionScript.replace(RPL_MARK, "result = " + expression);
		Binding biding = new Binding();
		biding.setVariable("input", expression);
		GroovyClassLoader loader = scriptEngine.getGroovyClassLoader();
		GroovyShell shell = new GroovyShell(loader, biding);
		try {
			String result = (String) shell.evaluate(script);
			return result;
		} catch (Exception e) {
			return null;
		}
	}
	
	private void loadScriptEngine() {
		synchronized (RPL_MARK) {
			if(scriptEngine==null) {
				ClassLoader ccl = Thread.currentThread().getContextClassLoader();
				scriptEngine = new GroovyScriptEngine(new URL[]{ccl.getResource("step/expressions/placeholder/")});
			}
		}
	}
	
	private void loadGroovyDatePlaceholder() {
		synchronized (RPL_MARK) {
			if(datePlaceholderScript==null) {
				try {
					datePlaceholderScript = (GroovyObject) scriptEngine.loadScriptByName("DatePlaceholders.groovy").newInstance();
				} catch (CompilationFailedException | InstantiationException | IllegalAccessException | ResourceException | ScriptException e) {
					throw new RuntimeException("Unable to load groovy DatePlaceholder.", e);
				}
			}
		}
	}
	
	private void loadGroovyTimeCategoryScript() {
		synchronized (RPL_MARK) {
			if(timeCategoryExpressionScript == null) {
				try {
					StringBuilder sb = new StringBuilder();
					ClassLoader ccl = Thread.currentThread().getContextClassLoader();
					InputStream is = ccl.getResourceAsStream("step/expressions/placeholder/TimeCategoryExpressionHandler.groovy");
					BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
			
					String line;
					line = br.readLine();
					while (line != null) {
						sb.append(line).append('\n');
						line = br.readLine();
					}
			
					timeCategoryExpressionScript = sb.toString();
				} catch (IOException e) {
					throw new RuntimeException("Unable to load groovy TimeCategory script.", e);
				}
			}
		}
	}
}
