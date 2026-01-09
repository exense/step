package step.expressions;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tokenization system for securely handling protected values in string concatenation operations.
 * <p>
 * This class provides a mechanism to temporarily replace sensitive values with unique tokens during
 * Groovy expression evaluation, then render them back to either their clear or obfuscated form
 * as needed. This prevents sensitive data from being directly manipulated or exposed in string
 * operations while maintaining the ability to produce both secure (obfuscated) and internal
 * (clear) representations.
 * </p>
 * 
 * <h3>Token Format:</h3>
 * <p>Tokens follow the pattern: {@code ⟦PB:uuid⟧} where the UUID uniquely identifies the
 * protected value pair (clear and obfuscated versions).</p>
 * 
 * <h3>Usage Pattern:</h3>
 * <pre>{@code
 * Tokenizer tokenizer = new Tokenizer();
 * 
 * // Create a token for a protected value
 * String token = tokenizer.tokenFor("sensitive", "***key***");
 * 
 * // Use token in string operations
 * String result = "prefix " + token + " suffix";
 * 
 * // Render back to desired form
 * String clearResult = tokenizer.render(result, true);     // "prefix sensitive suffix"
 * String obfResult = tokenizer.render(result, false);      // "prefix ***key*** suffix"
 * 
 * // Or get both forms as ProtectedVariable
 * Object both = tokenizer.renderBoth(result);              // ProtectedVariable instance
 * }</pre>
 * 
 * @see ProtectedVariable
 * @see ProtectionContext
 * @since 1.0
 */
final class Tokenizer {
    /**
     * Token prefix used in the tokenization format.
     */
    private static final String PREFIX = "⟦PB:";
    
    /**
     * Token suffix used in the tokenization format.
     */
    private static final String SUFFIX = "⟧";
    
    /**
     * Regular expression pattern for matching tokens in strings.
     * Captures the UUID portion of the token for replacement operations.
     */
    static final Pattern TOKEN = Pattern.compile("⟦PB:([0-9a-fA-F-]{36})⟧");

    /**
     * Request-scoped mapping from token UUIDs to their corresponding clear and obfuscated value pairs.
     */
    private final Map<String, Pair> map = new HashMap<>();

    /**
     * Immutable pair containing the clear (unobfuscated) and obfuscated representations of a protected value.
     */
    static final class Pair {
        /**
         * The clear (unobfuscated) value.
         */
        final String clear;
        
        /**
         * The obfuscated value.
         */
        final String obf;
        
        /**
         * Creates a new pair with clear and obfuscated values.
         * 
         * @param c the clear value
         * @param o the obfuscated value
         */
        Pair(String c, String o) { 
            clear = c; 
            obf = o; 
        }
    }

    /**
     * Creates a unique token for a protected value pair.
     * <p>
     * Generates a UUID-based token that can be used as a placeholder for the protected value
     * in string operations. The token maintains the association between the clear and obfuscated
     * forms of the value for later rendering.
     * </p>
     * 
     * @param clear the clear (unobfuscated) value
     * @param obf the obfuscated value
     * @return a unique token in the format {@code ⟦PB:uuid⟧}
     * 
     * @see #render(String, boolean)
     * @see #renderBoth(String)
     */
    String tokenFor(String clear, String obf) {
        String id = UUID.randomUUID().toString();
        map.put(id, new Pair(clear, obf));
        return PREFIX + id + SUFFIX;
    }

    /**
     * Renders a tokenized string by replacing all tokens with their corresponding values.
     * <p>
     * Searches for all tokens in the input string and replaces them with either their
     * clear or obfuscated forms based on the {@code asClear} parameter. If no tokens
     * are registered, the original string is returned unchanged.
     * </p>
     * 
     * @param token the string potentially containing tokens to be replaced
     * @param asClear {@code true} to render clear values, {@code false} for obfuscated values
     * @return the string with all tokens replaced by their corresponding values
     * @throws ProtectedPropertyException if token integrity validation fails
     * 
     * @see #tokenFor(String, String)
     * @see #renderBoth(String)
     */
    String render(String token, boolean asClear) {
        if (!map.isEmpty()) {
            assertTokensArePresent(token);
            Matcher m = TOKEN.matcher(token);
            StringBuffer out = new StringBuffer(token.length());
            while (m.find()) {
                Pair p = map.get(m.group(1));
                String rep = (p == null) ? m.group(0) : (asClear ? p.clear : p.obf);
                m.appendReplacement(out, Matcher.quoteReplacement(rep));
            }
            m.appendTail(out);
            return out.toString();
        } else {
            return token;
        }
    }

    /**
     * Validates that all registered tokens are present in the given string.
     * <p>
     * This security measure ensures that tokens haven't been tampered with or removed
     * during string operations, which could indicate malicious manipulation of protected values.
     * </p>
     * 
     * @param token the string to validate for token integrity
     * @throws ProtectedPropertyException if any registered tokens are missing from the string
     */
    private void assertTokensArePresent(String token) {
        boolean valid = map.keySet().stream().map(t -> PREFIX + t + SUFFIX).allMatch(token::contains);
        if (!valid) {
            throw new ProtectedPropertyException("Protected bindings have been tempered. This may indicate that unauthorized string manipulation methods (like substring, replace, or trim) " +
                        "were used on protected variables.");
        }
    }

    /**
     * Renders a tokenized string to both clear and obfuscated forms, returning a ProtectedVariable if tokens are present.
     * <p>
     * This method is a convenience function that determines whether the input string contains tokens
     * that need processing. If tokens are found and successfully replaced, it returns a
     * {@link ProtectedVariable} containing both the clear and obfuscated versions. If no tokens
     * are present, the original string is returned unchanged.
     * </p>
     * 
     * @param token the string potentially containing tokens to be rendered
     * @return a {@link ProtectedVariable} containing both clear and obfuscated forms if tokens were processed,
     *         or the original string if no token processing was needed
     * @throws ProtectedPropertyException if token integrity validation fails
     * 
     * @see #render(String, boolean)
     * @see #tokenFor(String, String)
     * @see ProtectedVariable
     */
    Object renderBoth(String token) {
        //only perform the rendering if tokens have been registered
        if (!map.isEmpty()) {
            String obfuscatedValue = render(token, false);
            //Make sure replacement were really required
            if (!obfuscatedValue.equals(token)) {
                return new ProtectedVariable(null, render(token, true), obfuscatedValue);
            }
        }
        //In all other cases the string contains no token and can be return as string directly
        return token;
    }
}