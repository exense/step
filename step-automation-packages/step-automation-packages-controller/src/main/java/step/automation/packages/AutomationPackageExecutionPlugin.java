package step.automation.packages;

import step.core.execution.ExecutionContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@IgnoreDuringAutoDiscovery
@Plugin
public class AutomationPackageExecutionPlugin extends AbstractExecutionEnginePlugin {

    private final static String AUTOMATION_PACKAGE_LOCK = "$automation.package.lock";
    private final AutomationPackageLocks automationPackageLocks;

    public AutomationPackageExecutionPlugin(AutomationPackageLocks automationPackageLocks) {
        this.automationPackageLocks = automationPackageLocks;
    }

    @Override
    public void executionStart(ExecutionContext context) {
        super.executionStart(context);
        String automationPackageId = (String) context.getPlan().getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID);
        if (automationPackageId != null) {
            automationPackageLocks.readLock(automationPackageId);
        }
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        super.afterExecutionEnd(context);
        String automationPackageId = (String) context.getPlan().getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID);
        if (automationPackageId != null) {
            automationPackageLocks.readUnlock(automationPackageId);
        }
    }
}
