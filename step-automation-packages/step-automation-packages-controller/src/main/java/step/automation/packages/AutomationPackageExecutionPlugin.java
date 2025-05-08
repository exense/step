package step.automation.packages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.core.repositories.RepositoryObjectReference;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;

import static step.automation.packages.AutomationPackageLocks.*;
import static step.repositories.LocalRepository.getPlanId;

/**
 * This execution plugin is used to prevent the update of automation packages currently in use by execution
 * Read lock on the package is started at when initializing the execution and released when execution ends in finalizeExecutionContext
 * It must run before FunctionPlugin.initializeExecutionContext which create the cached accessor for keywords
 */
@IgnoreDuringAutoDiscovery
@Plugin(runsBefore= FunctionPlugin.class)
public class AutomationPackageExecutionPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(AutomationPackageExecutionPlugin.class);
    private final AutomationPackageLocks automationPackageLocks;
    private static final String EXECUTION_CONTEXT_LOCK_ID = "EXECUTION_CONTEXT_LOCK_ID";

    public AutomationPackageExecutionPlugin(AutomationPackageLocks automationPackageLocks) {
        this.automationPackageLocks = automationPackageLocks;
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        super.initializeExecutionContext(executionEngineContext, executionContext);
        AutomationPackageExecutionContext automationPackageExecutionContext = new AutomationPackageExecutionContext(executionContext);
        if (shouldLock(automationPackageExecutionContext)) {
            try {
                debugLog(automationPackageExecutionContext, "Trying to acquire read lock on automation package.");
                boolean locked = automationPackageLocks.tryReadLock(automationPackageExecutionContext.automationPackageId);
                if (!locked) {
                    debugLog(automationPackageExecutionContext, "Timeout while acquiring read lock on automation package.");
                    throw new PluginCriticalException("Timeout while acquiring lock on automation package with id " +
                            automationPackageExecutionContext.automationPackageId + ". This usually means that an update of this automation package is on-going and took more than the property " +
                            AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS + " (default " + AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT + " seconds)");
                } else {
                    executionContext.put(EXECUTION_CONTEXT_LOCK_ID, automationPackageExecutionContext.automationPackageId);
                    debugLog(automationPackageExecutionContext, "Acquired read lock on automation package.");
                }
            } catch (InterruptedException e) {
                throw new PluginCriticalException("Thread interrupted while acquiring lock on automation package with id " + automationPackageExecutionContext.automationPackageId);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No lock required for plan {}", automationPackageExecutionContext.planName);
            }
        }
    }

    @Override
    public void finalizeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        super.finalizeExecutionContext(executionEngineContext, executionContext);
        AutomationPackageExecutionContext automationPackageExecutionContext = new AutomationPackageExecutionContext(executionContext);
        Object lockId = executionContext.get(EXECUTION_CONTEXT_LOCK_ID);
        if (lockId instanceof String) {
            String automationPackageId = (String) lockId;
            try {
                automationPackageLocks.readUnlock(automationPackageId);
                debugLog(automationPackageExecutionContext, "Released read lock on automation package.");
            } catch (java.lang.IllegalMonitorStateException e) {
                //occurs whenever the lock was not acquired in execution start due to timeout
                debugLog(automationPackageExecutionContext, "No read lock to be released for automation package.");
            }
        }
    }

    private void debugLog(AutomationPackageExecutionContext apContext, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message + " Id: " + apContext.automationPackageId + ", execution id: " +
                    apContext.context.getExecutionId() + ",  plan: " + apContext.planName);
        }
    }

    /**
     * Extract automation package info from context, if the context is related to an AP, the automationPackageId
     * is set. The planName is resolved too but only used for debugging.
     */
    public static class AutomationPackageExecutionContext {
        public final String automationPackageId;
        public final String planName;
        public final ExecutionContext context;

        public AutomationPackageExecutionContext(ExecutionContext context) {
            this.context = context;
            String apID = null;
            String resolvedPlanName = "unresolved";
            ExecutionParameters executionParameters = context.getExecutionParameters();
            if (executionParameters != null) {
                //Either it is already set in execution parameters (which is the case for schedules)
                apID = (String) executionParameters.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID);
                resolvedPlanName = (executionParameters.getPlan() != null) ?
                        executionParameters.getPlan().getAttribute(AbstractOrganizableObject.NAME) :
                        executionParameters.getDescription();
                if (apID == null) {
                    //Or it can be set in plan which is already in the parameters
                    if (executionParameters.getPlan() != null) {
                        apID = (String) executionParameters.getPlan().getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID);
                    //Last possibility is for local repo where the plan was deployed in DB
                    } else if (executionParameters.getRepositoryObject().getRepositoryID().equals(RepositoryObjectReference.LOCAL_REPOSITORY_ID)) {
                        String planId = getPlanId(executionParameters.getRepositoryObject().getRepositoryParameters());
                        Plan plan = context.getPlanAccessor().get(planId);
                        if (plan != null) {
                            apID = (String) plan.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID) ;
                            resolvedPlanName =  plan.getAttribute(AbstractOrganizableObject.NAME);
                        }
                    }
                }
            }
            automationPackageId = apID;
            planName = resolvedPlanName;
        }
    }

    public boolean shouldLock(AutomationPackageExecutionContext automationPackageExecutionContext) {
        if (automationPackageExecutionContext.automationPackageId == null) {
            return false;
        } else {
            Object byPassId = automationPackageExecutionContext.context.getExecutionParameters().getCustomField(BYPASS_AUTOMATION_PACKAGE_LOCK_FOR_ID);
            if (byPassId == null) {
                return true;
            } else if (byPassId instanceof String) {
                //Bypass lock if same automation package is already locked
                return !automationPackageExecutionContext.automationPackageId.equals(byPassId);
            } else {
                throw new RuntimeException(BYPASS_AUTOMATION_PACKAGE_LOCK_FOR_ID + " is set in context but its value is not a string: " + byPassId);
            }
        }
    }

}
