package step.core.yaml.deserialization;

public class AutomationPackageUpdateException extends RuntimeException {
    public AutomationPackageUpdateException(String s, Exception e) {
        super(s, e);
    }

    public AutomationPackageUpdateException(String s) {
        super(s);
    }
}
