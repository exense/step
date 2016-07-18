package step.expressions;


public class GroovyPoolKey {
	
	private final String script;

	public GroovyPoolKey(String script) {
		super();
		this.script = script;
	}

	public String getScript() {
		return script;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((script == null) ? 0 : script.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GroovyPoolKey other = (GroovyPoolKey) obj;
		if (script == null) {
			if (other.script != null)
				return false;
		} else if (!script.equals(other.script))
			return false;
		return true;
	}

}
