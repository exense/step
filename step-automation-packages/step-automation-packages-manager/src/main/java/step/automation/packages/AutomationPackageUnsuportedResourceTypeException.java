package step.automation.packages;


import java.util.Set;

public class AutomationPackageUnsuportedResourceTypeException extends Exception {

    public AutomationPackageUnsuportedResourceTypeException(String message, Set<String> supportedType) {
        super("Unsupported resource type: '" + message + "'. Supported types are: " + supportedType);
    }
}
