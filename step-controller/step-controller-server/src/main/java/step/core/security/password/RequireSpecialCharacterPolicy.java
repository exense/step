package step.core.security.password;

import ch.exense.commons.app.Configuration;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequireSpecialCharacterPolicy extends RegexPasswordPolicy {
    private static final String KEY_SPECIAL_CHARS = "requireSpecialCharacter";

    private final String requiredCharacters;

    public RequireSpecialCharacterPolicy(String requiredCharacters) {
        super(transformToRegex(requiredCharacters));
        this.requiredCharacters = requiredCharacters;
    }

    private static String transformToRegex(String chars) {
        String content = Arrays.stream(chars.split("")).map(c -> toRegex(c)).collect(Collectors.joining());
        return ".*[" + content + "].*";
    }

    // the argument is actually a single character
    private static String toRegex(String c) {
        int codepoint = c.codePointAt(0);
        return String.format("\\u%1$04X", codepoint);
    }

    @Override
    protected String getExceptionReason() {
        return "The password must contain at least one special character from the list: " + requiredCharacters;
    }

    @Override
    protected String getDescription() {
        return "at least one of the following special characters: " + requiredCharacters;
    }

    public static Optional<PasswordPolicy> from(Configuration configuration) {
        String required = configuration.getProperty(getConfigurationKey(KEY_SPECIAL_CHARS), null);
        if (required == null || required.trim().length() < 1) {
            return Optional.empty();
        }
        return Optional.of(new RequireSpecialCharacterPolicy(required.trim()));
    }
}
