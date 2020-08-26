package step.core.plugins.exceptions;

public class PluginCriticalException extends RuntimeException {

	private static final long serialVersionUID = -2190431494710290312L;

	public PluginCriticalException(String message) {
		super(message);
	}

	public PluginCriticalException(String string, Throwable e) {
		super(string, e);
	}

}
