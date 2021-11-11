package step.core.security.password;

import ch.exense.commons.app.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PasswordPolicies {
    private final List<PasswordPolicy> implementations;

    public PasswordPolicies(Configuration configuration) {
        List<Optional<PasswordPolicy>> policies = List.of(
                MinimumLengthPolicy.from(configuration),
                RequireLowerCasePolicy.from(configuration),
                RequireUpperCasePolicy.from(configuration),
                RequireNumericPolicy.from(configuration)
        );

        implementations = policies.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    public void verifyPassword(String password) throws PasswordPolicyViolation {
        for (PasswordPolicy policy: implementations) {
            policy.verify(password);
        }
    }
}
