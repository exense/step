package step.core.security.password;

import ch.exense.commons.app.Configuration;

import java.util.Optional;

public class MinimumLengthPolicy extends RegexPasswordPolicy {
    private static final String KEY_MINIMUM_LENGTH = "minimumLength";

    private final int minimumLength;

    @Override
    protected String getExceptionReason() {
        return "The password must have a length of at least " + minimumLength + " characters";
    }

    @Override
    protected String getDescription() {
        return "at least " + minimumLength + " characters";
    }

    public static Optional<PasswordPolicy> from(Configuration configuration) {
        Integer value = configuration.getPropertyAsInteger(getConfigurationKey(KEY_MINIMUM_LENGTH), null);
        return Optional.ofNullable(value != null ? new MinimumLengthPolicy(Math.max(0, value)) : null);
    }

    public MinimumLengthPolicy(int minimumLength) {
        super(".{" + minimumLength + ",}");
        this.minimumLength = minimumLength;
    }
}
