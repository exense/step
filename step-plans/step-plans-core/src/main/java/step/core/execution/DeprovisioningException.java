package step.core.execution;

import step.core.plugins.exceptions.PluginCriticalException;

public class DeprovisioningException extends PluginCriticalException {
    public DeprovisioningException(String string, Throwable e) {
        super(string, e);
    }
}
