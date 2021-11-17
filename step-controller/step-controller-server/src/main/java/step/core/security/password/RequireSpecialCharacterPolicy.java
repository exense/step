package step.core.security.password;

import ch.exense.commons.app.Configuration;

import java.util.Optional;

public class RequireSpecialCharacterPolicy extends PasswordPolicy {
    private static final String KEY_SPECIAL_CHARS = "requireSpecialCharacter";

    private final String requiredCharacters;

    public RequireSpecialCharacterPolicy(String requiredCharacters) {
        this.requiredCharacters = requiredCharacters;
    }

    @Override
    public void verify(String password) throws PasswordPolicyViolation {
        for (String c : requiredCharacters.split("")) {
            if (password.contains(c)) {
                return;
            }
        }
        throw new PasswordPolicyViolation("The password must contain at least one special character");
    }

    @Override
    public PasswordPolicyDescriptor getDescriptor() {
        PasswordPolicyDescriptor d = new PasswordPolicyDescriptor();
        d.rule = ".*FIXME.*";
        d.description = "The text \"FIXME\" (special characters WIP)";
        return d;
    }

    public static Optional<PasswordPolicy> from(Configuration configuration) {
        String required = configuration.getProperty(getConfigurationKey(KEY_SPECIAL_CHARS), null);
        return Optional.ofNullable(required != null ? new RequireSpecialCharacterPolicy(required.trim()) : null);
    }
}
