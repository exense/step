package step.commons.activation;

import javax.script.CompiledScript;

public class Expression {

	String script;
	
	String scriptEngine;
	
	CompiledScript compiledScript;

	public Expression() {
		super();
	}

	public Expression(String script) {
		super();
		this.script = script;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getScriptEngine() {
		return scriptEngine;
	}

	public void setScriptEngine(String scriptEngine) {
		this.scriptEngine = scriptEngine;
	}
	
}
