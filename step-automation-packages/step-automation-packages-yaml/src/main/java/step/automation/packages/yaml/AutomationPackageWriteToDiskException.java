package step.automation.packages.yaml;

import step.core.yaml.deserialization.AutomationPackageUpdateException;

public class AutomationPackageWriteToDiskException extends AutomationPackageUpdateException {
    public AutomationPackageWriteToDiskException(String s, Exception e) {
        super(s, e);
    }
}
