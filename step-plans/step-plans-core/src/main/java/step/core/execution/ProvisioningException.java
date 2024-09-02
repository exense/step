package step.core.execution;

import step.core.plugins.exceptions.PluginCriticalException;

public class ProvisioningException extends PluginCriticalException {

    public ProvisioningException(String message) {
        super(message);
    }

    public ProvisioningException(String string, Throwable e) {
        super(string, e);
    }
}
