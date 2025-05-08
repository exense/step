package step.automation.packages;

public class AutomationPackageHookException extends RuntimeException {

    public AutomationPackageHookException(String message) {
        super(message);
    }

    public AutomationPackageHookException(String message, Throwable cause) {
        super(message, cause);
    }
}
