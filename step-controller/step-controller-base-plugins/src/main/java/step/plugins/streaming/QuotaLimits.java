package step.plugins.streaming;

import ch.exense.commons.app.Configuration;
import step.core.artefacts.reports.ReportNode;
import step.core.variables.VariablesManager;
import step.plugins.streaming.util.NumberCoercions;

import java.util.Objects;

import static step.plugins.streaming.QuotaLimits.ConfigurationKeys.*;

public class QuotaLimits {
    public static final class ConfigurationKeys {
        public static final String MAX_RESOURCES_PER_EXECUTION = "reporting.attachments.streaming.quota.maxResourcesPerExecution";
        public static final String MAX_BYTES_PER_RESOURCE = "reporting.attachments.streaming.quota.maxBytesPerResource";
        public static final String MAX_BYTES_PER_EXECUTION = "reporting.attachments.streaming.quota.maxBytesPerExecution";
        public static final String RUNTIME_OVERRIDABLE = "reporting.attachments.streaming.quota.runtimeOverridable";

        private ConfigurationKeys() {
        }
    }

    public final Long maxBytesPerResource;
    public final Long maxBytesPerExecution;
    // note: Integer is more than enough for the domain here, but since almost all calculations use longs,
    // we reuse them instead of duplicating them for Integer; just needs a few up/down-casts as a consequence
    public final Integer maxResourcesPerExecution;
    public final boolean runtimeOverridable;

    public QuotaLimits(Long maxBytesPerResource, Long maxBytesPerExecution, Long maxResourcesPerExecution, boolean runtimeOverridable) {
        this.maxBytesPerResource = maxBytesPerResource;
        this.maxBytesPerExecution = maxBytesPerExecution;
        this.maxResourcesPerExecution = maxResourcesPerExecution == null ? null : (int) maxResourcesPerExecution.longValue();
        this.runtimeOverridable = runtimeOverridable;
    }

    public boolean isUnlimited() {
        return maxBytesPerExecution == null && maxBytesPerResource == null && maxResourcesPerExecution == null;
    }

    @Override
    public String toString() {
        return "QuotaLimits{" +
                "maxBytesPerResource=" + maxBytesPerResource +
                ", maxBytesPerExecution=" + maxBytesPerExecution +
                ", maxResourcesPerExecution=" + maxResourcesPerExecution +
                ", runtimeOverridable=" + runtimeOverridable +
                '}';
    }

    private static Long negativeToNull(Long value) {
        return value == null ? null : value < 0 ? null : value;
    }

    public static QuotaLimits fromStepProperties(Configuration conf) {
        // Here is where the defaults are defined (-1 -> null == no limit)
        Long maxBytesPerResource = negativeToNull(conf.getPropertyAsLong(MAX_BYTES_PER_RESOURCE, -1L));
        Long maxBytesPerExecution = negativeToNull(conf.getPropertyAsLong(MAX_BYTES_PER_EXECUTION, 500_000_000L));
        Long maxResourcesPerExecution = negativeToNull(conf.getPropertyAsLong(MAX_RESOURCES_PER_EXECUTION, -1L));
        // Users have to explicitly opt in in step.properties to allow runtime overriding
        boolean runtimeOverridable = conf.getPropertyAsBoolean(RUNTIME_OVERRIDABLE, false);
        return new QuotaLimits(maxBytesPerResource, maxBytesPerExecution, maxResourcesPerExecution, runtimeOverridable);
    }

    public static QuotaLimits fromVariables(VariablesManager variables, ReportNode reportNode, QuotaLimits fallback) {
        Long maxBytesPerResource = longVarWithFallback(variables, reportNode, MAX_BYTES_PER_RESOURCE, fallback.maxBytesPerResource);
        Long maxBytesPerExecution = longVarWithFallback(variables, reportNode, MAX_BYTES_PER_EXECUTION, fallback.maxBytesPerExecution);
        // need to do the type dance again
        Long maxResPerExecution = longVarWithFallback(variables, reportNode, MAX_RESOURCES_PER_EXECUTION, fallback.maxResourcesPerExecution != null ? (long) fallback.maxResourcesPerExecution : null);
        return new QuotaLimits(maxBytesPerResource, maxBytesPerExecution, maxResPerExecution, false);
    }

    // Retrieves the variable from variables manager if present, otherwise returns fallback.
    // The same logic of treating negative as unlimited (thus returning null) is applied.
    private static Long longVarWithFallback(VariablesManager variables, ReportNode reportNode, String variableName, Long fallback) {
        Long maybeValue = NumberCoercions.asLong(variables.getVariable(reportNode, variableName, true));
        if (maybeValue == null) {
            return fallback;
        } else {
            return negativeToNull(maybeValue);
        }
    }

    // This is "almost" the same as .equals(...) would be, but without accounting for the overridable flag;
    // (that's why it wasn't implemented as .equals(...), it would be slightly misleading)
    public static boolean areSame(QuotaLimits l1, QuotaLimits l2) {
        return Objects.equals(l1.maxBytesPerResource, l2.maxBytesPerResource)
                && Objects.equals(l1.maxBytesPerExecution, l2.maxBytesPerExecution)
                && Objects.equals(l1.maxResourcesPerExecution, l2.maxResourcesPerExecution);
    }


}
