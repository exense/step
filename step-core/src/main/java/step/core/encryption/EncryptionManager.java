package step.core.encryption;

public interface EncryptionManager {

	String encrypt(String value) throws EncryptionManagerException;

	String decrypt(String encryptedValue) throws EncryptionManagerException;
	
	/**
	 * @return true if the key pair of this encryption manager has changed and that
	 *         a re-encryption is required
	 */
	boolean isKeyPairChanged();
	
	/**
	 * @return true if this encryption manager is starting for the first time and
	 *         that initial encryption is required
	 */
	boolean isFirstStart();

}