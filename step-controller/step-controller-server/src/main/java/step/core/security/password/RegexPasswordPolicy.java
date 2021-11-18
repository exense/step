package step.core.security.password;

public abstract class RegexPasswordPolicy extends PasswordPolicy {

    private final String regex;

    public RegexPasswordPolicy(String regex) {
        this.regex = regex;
    }

    protected boolean matches(String password) {
        return password != null && password.matches(regex);
    }

    @Override
    public PasswordPolicyDescriptor getDescriptor() {
        PasswordPolicyDescriptor d = new PasswordPolicyDescriptor();
        d.description = getDescription();
        d.rule = regex;
        return d;
    }

    @Override
    public final void verify(String password) throws PasswordPolicyViolation {
        if (!password.matches(regex)) {
            throw new PasswordPolicyViolation(getExceptionReason());
        }
    }

    protected abstract String getExceptionReason();

    protected abstract String getDescription();

}
