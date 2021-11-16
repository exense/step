package step.core.security.password;

import ch.exense.commons.app.Configuration;

import java.util.Optional;

public class MinimumLengthPolicy extends PasswordPolicy {
    private static final String KEY_MINIMUM_LENGTH = "minimumLength";

    private final int minimumLength;

    @Override
    public void verify(String password) throws PasswordPolicyViolation {
        if (password == null || password.length() < minimumLength) {
            throw new PasswordPolicyViolation("The password must have a length of at least " + minimumLength + " characters");
        }
    }

    public static Optional<PasswordPolicy> from(Configuration configuration) {
        Integer value = configuration.getPropertyAsInteger(getConfigurationKey(KEY_MINIMUM_LENGTH), null);
        return Optional.ofNullable(value != null ? new MinimumLengthPolicy(value) : null);
    }

    public MinimumLengthPolicy(int minimumLength) {
        this.minimumLength = minimumLength;
    }
}
