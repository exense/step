package step.automation.packages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import static step.automation.packages.AutomationPackagePlugin.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS;
import static step.automation.packages.AutomationPackagePlugin.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT;

/**
 * This execution plugin is used to prevent the update of automation packages currently in use by execution
 * Read lock on the package is started at execution start time and released when execution ends
 */
@IgnoreDuringAutoDiscovery
@Plugin
public class AutomationPackageExecutionPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(AutomationPackageExecutionPlugin.class);
    private final AutomationPackageLocks automationPackageLocks;

    public AutomationPackageExecutionPlugin(AutomationPackageLocks automationPackageLocks) {
        this.automationPackageLocks = automationPackageLocks;
    }

    @Override
    public void executionStart(ExecutionContext context) {
        super.executionStart(context);
        String automationPackageId = getAutomationPackageId(context);
        if (automationPackageId != null) {
            try {
                boolean locked = automationPackageLocks.tryReadLock(automationPackageId);
                if (!locked) {
                    logger.debug("Timeout while acquiring read lock on automation package " + automationPackageId);
                    throw new PluginCriticalException("Timeout while acquiring lock on automation package with id " +
                            automationPackageId + ". This usually means that an update of this automation package is on-going and took more than the property " +
                            AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS + " (default " + AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT + " seconds)");
                }
                logger.debug("Acquired read lock on automation package " + automationPackageId);
            } catch (InterruptedException e) {
                throw new PluginCriticalException("Thread interrupted while acquiring lock on automation package with id " + automationPackageId);
            }
        }
    }

    private static String getAutomationPackageId(ExecutionContext context) {
        Plan plan = context.getPlan();
        return (plan != null) ? (String) plan.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID):
                null;
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        super.afterExecutionEnd(context);
        String automationPackageId = getAutomationPackageId(context);
        if (automationPackageId != null) {
            try {
                automationPackageLocks.readUnlock(automationPackageId);
                logger.debug("Released read lock on automation package " + automationPackageId);
            } catch (java.lang.IllegalMonitorStateException e) {
                //occurs whenever the lock was not acquired in execution start due to timeout
                logger.debug("No read lock to be released for automation package " + automationPackageId);
            }
        }
    }
}
