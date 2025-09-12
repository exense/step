package step.expressions;

import static step.expressions.GroovyProtectedBinding.obfuscate;

/**
 * A container for sensitive data that maintains both clear and obfuscated representations.
 * <p>
 * This class serves as the primary data structure for handling protected values throughout
 * the expression evaluation system. It acts as both an input and output format for secure
 * data handling in Groovy expressions, ensuring that sensitive information is properly
 * protected while maintaining usability.
 * </p>
 * 
 * <h3>System Integration:</h3>
 * <p>
 * ProtectedVariable instances are typically created from protected {@link step.parameter.Parameter}
 * objects within the Java application. The lifecycle follows this pattern:
 * </p>
 * <ol>
 *   <li><strong>Creation:</strong> Protected Parameters are converted to ProtectedVariable instances</li>
 *   <li><strong>Groovy Binding:</strong> ProtectedVariables are converted to {@link GroovyProtectedBinding}
 *       objects and added to the Groovy evaluation bindings</li>
 *   <li><strong>Expression Evaluation:</strong> Groovy expressions can safely reference and manipulate 
 *       these protected bindings</li>
 *   <li><strong>Result Processing:</strong> If the evaluation result uses any protected bindings, 
 *       it is wrapped back into a ProtectedVariable for safe handling</li>
 * </ol>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Dual Representation:</strong> Maintains both clear and obfuscated forms</li>
 *   <li><strong>Safe toString():</strong> Always returns the obfuscated value</li>
 *   <li><strong>Immutable:</strong> All fields are final to prevent modification</li>
 *   <li><strong>Flexible Values:</strong> Can contain any object type as the clear value</li>
 *   <li><strong>Expression Integration:</strong> Seamlessly works with Groovy expression evaluation</li>
 * </ul>
 *
 * <h3>Security Considerations:</h3>
 * <p>
 * This class provides protection against accidental exposure of sensitive data in logs,
 * error messages, and user interfaces. However, direct access to the {@link #value} field
 * will still expose the clear data, so care must be taken to control access to ProtectedVariable
 * instances and to avoid inadvertently accessing the value field in unsafe contexts.
 * </p>
 * 
 * @see GroovyProtectedBinding
 * @see Tokenizer
 * @see ProtectionContext
 * @see step.parameter.Parameter
 * @since 1.0
 */
public class ProtectedVariable {

    /**
     * The identifier or name associated with this protected value.
     * Used for generating default obfuscation patterns and debugging.
     * May be {@code null} for anonymous protected values.
     */
    public final String key;
    
    /**
     * The actual (clear) value being protected.
     * This field contains the real data and should be accessed with caution
     * to prevent accidental exposure in logs, error messages, or user interfaces.
     */
    public final Object value;
    
    /**
     * The obfuscated representation of the value.
     * This is the safe version that can be displayed in logs, error messages,
     * and user interfaces without revealing sensitive information.
     */
    public final String obfuscatedValue;

    /**
     * Creates a ProtectedVariable with automatic obfuscation based on the key.
     * <p>
     * The obfuscated value is automatically generated using the pattern {@code ***key***}.
     * If the key is {@code null}, a default obfuscation pattern will be used.
     * </p>
     * 
     * @param key the identifier for this protected value
     * @param value the actual value to be protected
     * 
     * @see #ProtectedVariable(String, Object, String)
     * @see GroovyProtectedBinding#obfuscate(String)
     */
    public ProtectedVariable(String key, Object value) {
        this(key, value, obfuscate(key));
    }

    /**
     * Creates a ProtectedVariable with a custom obfuscated representation.
     * <p>
     * This constructor allows full control over both the actual value and its
     * obfuscated representation, enabling custom obfuscation patterns beyond
     * the default {@code ***key***} format.
     * </p>
     * 
     * @param key the identifier for this protected value
     * @param value the actual value to be protected
     * @param obfuscatedValue the custom obfuscated representation
     * 
     * @see #ProtectedVariable(String, Object)
     */
    public ProtectedVariable(String key, Object value, String obfuscatedValue) {
        this.key = key;
        this.value = value;
        this.obfuscatedValue = obfuscatedValue;
    }

    /**
     * Returns the obfuscated representation of this protected value.
     * <p>
     * This method is crucial for security as it ensures that when ProtectedVariable
     * instances are accidentally converted to strings (in logging, error messages,
     * or debugging output), only the safe, obfuscated value is exposed, never the
     * actual sensitive data.
     * </p>
     * 
     * @return the obfuscated value, safe for display in logs and user interfaces
     * 
     * @see #obfuscatedValue
     */
    @Override
    public String toString() {
        return obfuscatedValue;
    }
}
