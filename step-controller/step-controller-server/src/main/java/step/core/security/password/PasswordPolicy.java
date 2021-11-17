package step.core.security.password;

public abstract class PasswordPolicy {
    public static String CONFIGURATION_PREFIX = "security.password.";

    public static String getConfigurationKey(String suffix) {
        return CONFIGURATION_PREFIX + suffix;
    }

    public abstract void verify(String password) throws PasswordPolicyViolation;

    public abstract PasswordPolicyDescriptor getDescriptor();
}
