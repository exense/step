package step.automation.packages;

/**
 * Automation package operation mode, indicating if an AutomationPackageManager
 * and its operations are in main (=production), isolated, or local context.
 */
public enum AutomationPackageOperationMode {
    MAIN,
    ISOLATED,
    LOCAL
}
