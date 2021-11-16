package step.core.security.password;

public class PasswordPolicyViolation extends Exception {
    public PasswordPolicyViolation(String reason) {
        super(reason);
    }
}
