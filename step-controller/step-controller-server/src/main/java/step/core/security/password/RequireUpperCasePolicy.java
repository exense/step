package step.core.security.password;

import ch.exense.commons.app.Configuration;

import java.util.Optional;

public class RequireUpperCasePolicy extends RegexPolicy {
    private static final String KEY_ENABLED = "requireUpperCase";


    public RequireUpperCasePolicy() {
        super(".*[A-Z].*");
    }

    @Override
    public void verify(String password) throws PasswordPolicyViolation {
        if (!matches(password)) {
            throw new PasswordPolicyViolation("The password must contain at least one uppercase character");
        }
    }

    public static Optional<PasswordPolicy> from(Configuration configuration) {
        boolean value = configuration.getPropertyAsBoolean(getConfigurationKey(KEY_ENABLED));
        return Optional.ofNullable(value ? new RequireUpperCasePolicy() : null);
    }

}
