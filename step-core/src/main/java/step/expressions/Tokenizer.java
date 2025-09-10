package step.expressions;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Tokenizer {
    private static final String PREFIX = "⟦PB:";
    private static final String SUFFIX = "⟧";
    static final Pattern TOKEN = Pattern.compile("⟦PB:([0-9a-fA-F-]{36})⟧");

    // request-scoped map: id -> (clear, obf)
    private final Map<String, Pair> map = new HashMap<>();

    static final class Pair {
        final String clear, obf;
        Pair(String c, String o) { clear = c; obf = o; }
    }

    String tokenFor(String clear, String obf) {
        String id = UUID.randomUUID().toString();
        map.put(id, new Pair(clear, obf));
        return PREFIX + id + SUFFIX;
    }

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

    private void assertTokensArePresent(String token) {
        boolean valid = map.keySet().stream().map(t -> PREFIX + t + SUFFIX).allMatch(token::contains);
        if (!valid) {
            throw new ProtectedPropertyException("Protected bindings have been tempered.");
        }
    }

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