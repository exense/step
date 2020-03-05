package step.core.plugins.exceptions;

public class PluginCriticalException extends RuntimeException {

	public PluginCriticalException(String string, Exception e) {
		super(string,e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2190431494710290312L;

}
