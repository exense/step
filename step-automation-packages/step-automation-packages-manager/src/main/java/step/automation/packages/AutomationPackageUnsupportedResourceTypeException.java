package step.automation.packages;


import java.util.Set;

public class AutomationPackageUnsupportedResourceTypeException extends Exception {

    public AutomationPackageUnsupportedResourceTypeException(String message, Set<String> supportedTypes) {
        super("Unsupported resource type: '" + message + "'. Supported types are: " + supportedTypes);
    }
}
