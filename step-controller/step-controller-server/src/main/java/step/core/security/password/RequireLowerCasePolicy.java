package step.core.security.password;

import ch.exense.commons.app.Configuration;

import java.util.Optional;

public class RequireLowerCasePolicy extends RegexPasswordPolicy {
    private static final String KEY_ENABLED = "requireLowerCase";

    public RequireLowerCasePolicy() {
        super(".*[a-z].*");
    }

    @Override
    protected String getExceptionReason() {
        return "The password must contain at least one lowercase character";
    }

    @Override
    protected String getDescription() {
        return "at least one lowercase character";
    }

    public static Optional<PasswordPolicy> from(Configuration configuration) {
        boolean value = configuration.getPropertyAsBoolean(getConfigurationKey(KEY_ENABLED));
        return Optional.ofNullable(value ? new RequireLowerCasePolicy() : null);
    }

}
