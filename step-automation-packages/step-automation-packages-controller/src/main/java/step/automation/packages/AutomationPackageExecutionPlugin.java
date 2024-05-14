package step.automation.packages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import static step.automation.packages.AutomationPackageLocks.BYPASS_AUTOMATION_PACKAGE_LOCK_FOR_ID;
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
    private static final String EXECUTION_CONTEXT_LOCK_ID = "EXECUTION_CONTEXT_LOCK_ID";

    public AutomationPackageExecutionPlugin(AutomationPackageLocks automationPackageLocks) {
        this.automationPackageLocks = automationPackageLocks;
    }

    @Override
    public void executionStart(ExecutionContext context) {
        super.executionStart(context);
        if (shouldLock(context)) {
            String automationPackageId = getAutomationPackageId(context);
            try {
                debugLog(context, "Trying to acquire read lock on automation package.");
                boolean locked = automationPackageLocks.tryReadLock(automationPackageId);
                if (!locked) {
                    debugLog(context, "Timeout while acquiring read lock on automation package.");
                    throw new PluginCriticalException("Timeout while acquiring lock on automation package with id " +
                            automationPackageId + ". This usually means that an update of this automation package is on-going and took more than the property " +
                            AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS + " (default " + AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT + " seconds)");
                }
                context.put(EXECUTION_CONTEXT_LOCK_ID, automationPackageId);
                debugLog(context,"Acquired read lock on automation package.");
            } catch (InterruptedException e) {
                throw new PluginCriticalException("Thread interrupted while acquiring lock on automation package with id " + automationPackageId);
            }
        }
    }

    private void debugLog(ExecutionContext context, String message) {
        if (logger.isDebugEnabled()) {
            Plan plan = context.getPlan();
            String planName = (plan != null) ? plan.getAttribute(AbstractOrganizableObject.NAME) : "unresolved";
            String automationPackageId = getAutomationPackageId(context);
            logger.debug(message + " Id: " + automationPackageId + ", execution id: " +
                    context.getExecutionId() + ",  plan: " + planName);
        }
    }

    public boolean shouldLock(ExecutionContext context) {
        String automationPackageId = getAutomationPackageId(context);
        if (automationPackageId == null) {
            return false;
        } else {
            Object byPassId = context.getExecutionParameters().getCustomField(BYPASS_AUTOMATION_PACKAGE_LOCK_FOR_ID);
            if (byPassId == null) {
                return true;
            } else if (byPassId instanceof String) {
                //Bypass lock if same automation package is already locked
                return !automationPackageId.equals(byPassId);
            } else {
                throw new RuntimeException(BYPASS_AUTOMATION_PACKAGE_LOCK_FOR_ID + " is set in context but its value is not a string: " + byPassId);
            }
        }
    }

    private static String getAutomationPackageId(ExecutionContext context) {
        Plan plan = context.getPlan();
        return (plan != null) ? (String) plan.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID):
                null;
    }

    @Override
    public void executionFinally(ExecutionContext context) {
        super.executionFinally(context);
        Object lockId = context.get(EXECUTION_CONTEXT_LOCK_ID);
        if (lockId != null && lockId instanceof String) {
            String automationPackageId = (String) lockId;
            try {
                automationPackageLocks.readUnlock(automationPackageId);
                debugLog(context, "Released read lock on automation package.");
            } catch (java.lang.IllegalMonitorStateException e) {
                //occurs whenever the lock was not acquired in execution start due to timeout
                debugLog(context, "No read lock to be released for automation package.");
            }
        }
    }
}
