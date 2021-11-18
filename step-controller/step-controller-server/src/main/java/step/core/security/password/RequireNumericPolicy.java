package step.core.security.password;

import ch.exense.commons.app.Configuration;

import java.util.Optional;

public class RequireNumericPolicy extends RegexPasswordPolicy {
    private static final String KEY_ENABLED = "requireNumeric";

    public RequireNumericPolicy() {
        super(".*[0-9].*");
    }

    @Override
    protected String getExceptionReason() {
        return "The password must contain at least one numeric character";
    }

    @Override
    protected String getDescription() {
        return "at least one numeric character";
    }

    public static Optional<PasswordPolicy> from(Configuration configuration) {
        boolean value = configuration.getPropertyAsBoolean(getConfigurationKey(KEY_ENABLED));
        return Optional.ofNullable(value ? new RequireNumericPolicy() : null);
    }

}
