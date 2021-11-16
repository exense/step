package step.core.security.password;

public abstract class RegexPolicy extends PasswordPolicy {

    private final String regex;

    public RegexPolicy(String regex) {
        this.regex = regex;
    }

    protected boolean matches(String password) {
        return password != null && password.matches(regex);
    }
}
