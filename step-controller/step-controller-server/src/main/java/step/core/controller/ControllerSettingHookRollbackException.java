package step.core.controller;

public class ControllerSettingHookRollbackException extends RuntimeException {
	public ControllerSettingHookRollbackException(String message) {
		super(message);
	}

	public ControllerSettingHookRollbackException(String message, Throwable cause) {
		super(message, cause);
	}
}
