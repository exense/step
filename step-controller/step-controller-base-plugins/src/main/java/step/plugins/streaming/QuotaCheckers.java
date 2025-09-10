package step.plugins.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.constants.StreamingConstants;
import step.core.artefacts.reports.ReportNode;
import step.core.variables.VariablesManager;
import step.streaming.common.QuotaExceededException;
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

    private QuotaLimits determineEffectiveLimits(String executionId, StreamingResourceUploadContext uploadContext) throws QuotaExceededException {
        // TODO: Step 30+ adjust for per-project overridable limits
        if (!globalLimits.runtimeOverridable) {
            return globalLimits;
        }
        try {
            VariablesManager variables = (VariablesManager) uploadContext.getAttributes().get(StreamingConstants.AttributeNames.VARIABLES_MANAGER);
            if (variables == null) {
                // Note that this should not normally happen, it would indicate some kind of serious bug
                logger.warn("No variables manager found while calculating quotas for execution id {}, this should not happen; returning global limits", executionId);
                throw new QuotaExceededException("UNEXPECTED: No variables manager found, quota check impossible");
            }
            ReportNode reportNode = (ReportNode) uploadContext.getAttributes().get(StreamingConstants.AttributeNames.REPORT_NODE);
            QuotaLimits overridden = QuotaLimits.fromVariables(variables, reportNode, globalLimits);
            if (logger.isDebugEnabled()) {
                if (!QuotaLimits.areSame(overridden, globalLimits)) {
                    logger.debug("Effective quota limits for execution {} were overridden at runtime to: {}", executionId, overridden);
                }
            }
            return overridden;
        } catch (QuotaExceededException e) {
            throw e;
        } catch (Exception e) {
            // Again, this should not normally happen, it would indicate some kind of serious bug
            logger.error("Unexpected error while determining quotas for execution id {}, refusing upload", executionId, e);
            throw new QuotaExceededException("UNEXPECTED error when determining quotas: " + e.getMessage() );
        }
    }

    // Required because we can't throw checked exceptions in closures
    private static final class QuotaExceededUnchecked extends RuntimeException {
        QuotaExceededUnchecked(QuotaExceededException cause) { super(cause); }
        // That synchronized modifier is present in super.getCause() anyway, so we just cleanly override the existing signature.
        // This doesn't add any additional overhead.
        @Override public synchronized QuotaExceededException getCause() {
            return (QuotaExceededException) super.getCause();
        }
    }

    // Note: there are no errors expected to occur, but in case there are, we throw an exception that will effectively refuse the uploads
    public QuotaChecker getForExecution(String executionId, StreamingResourceUploadContext uploadContext) throws QuotaExceededException {
        try {
            Optional<QuotaChecker> entry = perExecution.computeIfAbsent(executionId, id -> {
                try {
                    QuotaLimits limits = determineEffectiveLimits(id, uploadContext);
                    if (limits.isUnlimited()) {
                        // indicate that no quota enforcement is required
                        return Optional.empty();
                    }
                    return Optional.of(new QuotaChecker(id, limits));
                } catch (QuotaExceededException e) {
                    // Note that this should not normally happen, it would indicate some kind of serious bug
                    throw new QuotaExceededUnchecked(e);
                }
            });
            // empty -> null
            return entry.orElse(null);
        } catch (QuotaExceededUnchecked e) {
            throw e.getCause();
        }
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
