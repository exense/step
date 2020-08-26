package step.core;

import step.core.plugins.exceptions.PluginCriticalException;

@SuppressWarnings("serial")
public class DependencyException extends PluginCriticalException {

	public DependencyException(String string) {
		super(string);
	}

}
