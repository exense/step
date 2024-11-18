package step.parameter;

import step.core.encryption.EncryptionManagerException;

public class ParameterManagerException  extends RuntimeException {
    public ParameterManagerException(String s) {
        super(s);
    }

    public ParameterManagerException(String message, Exception e) {
        super(message, e);
    }
}
