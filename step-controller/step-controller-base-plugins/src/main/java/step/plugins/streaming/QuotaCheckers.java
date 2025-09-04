package step.plugins.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.constants.StreamingConstants;
import step.core.artefacts.reports.ReportNode;
import step.core.variables.VariablesManager;
import step.streaming.common.StreamingResourceUploadContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class QuotaCheckers {
    private static final Logger logger = LoggerFactory.getLogger(QuotaCheckers.class);
    private final QuotaLimits globalLimits;
    // This is an Optional<> to distinguish between "we don't know about this execution" (null)
    // and "we know this execution, but no quota checks are required" (Optional.EMPTY)
    private final Map<String, Optional<QuotaChecker>> perExecution = new ConcurrentHashMap<>();
    private final Map<String, QuotaChecker> perResource = new ConcurrentHashMap<>();

    public QuotaCheckers(QuotaLimits globalLimits) {
        this.globalLimits = globalLimits;
    }

    private QuotaLimits determineEffectiveLimits(String executionId, StreamingResourceUploadContext uploadContext) {
        // TODO: Step 30+ adjust for per-project overridable limits
        if (!globalLimits.runtimeOverridable) {
            return globalLimits;
        }
        try {
            VariablesManager variables = (VariablesManager) uploadContext.getAttributes().get(StreamingConstants.AttributeNames.VARIABLES_MANAGER);
            if (variables == null) {
                logger.warn("No variables manager found while calculating quotas for execution id {}, this should not happen; returning global limits", executionId);
                return globalLimits;
            }
            ReportNode reportNode = (ReportNode) uploadContext.getAttributes().get(StreamingConstants.AttributeNames.REPORT_NODE);
            QuotaLimits overridden = QuotaLimits.fromVariables(variables, reportNode, globalLimits);
            if (logger.isDebugEnabled()) {
                if (!QuotaLimits.areSame(overridden, globalLimits)) {
                    logger.debug("Effective quota limits for execution {} were overridden at runtime to: {}", executionId, overridden);
                }
            }
            return overridden;
        } catch (Exception e) {
            // better safe than sorry
            logger.error("Error while determining quotas for execution id {}, falling back to default system quota limits", executionId, e);
            return globalLimits;
        }
    }

    public QuotaChecker getForExecution(String executionId, StreamingResourceUploadContext uploadContext) {
        Optional<QuotaChecker> entry = perExecution.computeIfAbsent(executionId, id -> {
            QuotaLimits limits = determineEffectiveLimits(id, uploadContext);
            if (limits.isUnlimited()) {
                // indicate that no quota enforcement is required
                return Optional.empty();
            }
            return Optional.of(new QuotaChecker(id, limits));
        });
        // empty -> null
        return entry.orElse(null);
    }

    public void setForResource(String resourceId, QuotaChecker quotaChecker) {
        perResource.put(resourceId, quotaChecker);
    }

    public QuotaChecker getForResource(String resourceId) {
        return perResource.get(resourceId);
    }

    public void removeExecution(String executionId) {
        // execution is finished, so there will be no more calls to the respective checker. We can
        // safely access its internal state to remove the map keys for resources it tracked.
        Optional<QuotaChecker> quotaCheckerOpt = perExecution.remove(executionId);
        if (quotaCheckerOpt != null) {
            quotaCheckerOpt.ifPresent(quotaChecker -> {
                for (String resourceId : quotaChecker.resources.keySet()) {
                    perResource.remove(resourceId);
                }
            });
        }
    }
}
