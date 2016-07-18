package step.expressions;

import groovy.lang.Script;

public class GroovyPoolEntry {
	
	GroovyPoolKey key;
	
	Script script;

	public GroovyPoolEntry(GroovyPoolKey key, Script script) {
		super();
		this.key = key;
		this.script = script;
	}

	public Script getScript() {
		return script;
	}

	public void setScript(Script script) {
		this.script = script;
	}

	public GroovyPoolKey getKey() {
		return key;
	}

	public void setKey(GroovyPoolKey key) {
		this.key = key;
	}

}
