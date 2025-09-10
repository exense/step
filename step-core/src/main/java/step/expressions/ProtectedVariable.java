package step.expressions;

import static step.expressions.GroovyProtectedBinding.obfuscate;

public class ProtectedVariable {

    public final String key;
    public final Object value;
    public final String obfuscatedValue;

    public ProtectedVariable(String key, Object value) {
        this(key, value, obfuscate(key));
    }

    public ProtectedVariable(String key, Object value, String obfuscatedValue) {
        this.key = key;
        this.value = value;
        this.obfuscatedValue = obfuscatedValue;
    }

    @Override
    public String toString() {
        return obfuscatedValue;
    }
}
