package step.encryption;

public class EncryptedValueManagerException extends RuntimeException {
    public EncryptedValueManagerException(String s) {
        super(s);
    }

    public EncryptedValueManagerException(String message, Exception e) {
        super(message, e);
    }
}
