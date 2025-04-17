package step.core.settings;

public class SettingHookRollbackException extends RuntimeException {
	public SettingHookRollbackException(String message) {
		super(message);
	}

	public SettingHookRollbackException(String message, Throwable cause) {
		super(message, cause);
	}
}
