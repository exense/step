package step.core.encryption;

public interface EncryptionManager {

	String encrypt(String value) throws EncryptionManagerException;

	String decrypt(String encryptedValue) throws EncryptionManagerException;

}