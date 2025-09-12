/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.expressions;

/**
 * Thread-local context for managing protected value access during Groovy expression evaluation.
 * <p>
 * This class provides a secure mechanism to control whether protected bindings can be accessed
 * during the evaluation of Groovy expressions. It ensures that once a protection context is set
 * for a thread, it cannot be overridden, preventing privilege escalation attacks.
 * </p>
 * <p>
 * The context also includes a {@link Tokenizer} for processing protected value markers in
 * string concatenation operations, allowing secure handling of sensitive data in expressions.
 * </p>
 * 
 * <h3>Usage Pattern:</h3>
 * <pre>{@code
 * try {
 *     ProtectionContext.set(canAccessProtectedValues);
 *     // Perform Groovy expression evaluation
 *     Object result = evaluateExpression(expression, bindings);
 * } finally {
 *     ProtectionContext.clear();
 * }
 * }</pre>
 * 
 * @see ProtectedVariable
 * @see GroovyProtectedBinding
 * @see Tokenizer
 * @since 1.0
 */
public final class ProtectionContext {
    /**
     * Thread-local storage for the protection context instance.
     */
    private static final ThreadLocal<ProtectionContext> CONTEXT = new ThreadLocal<>();

    /**
     * Flag indicating whether protected values can be accessed in this context.
     */
    private final boolean canAccessProtectedValue;
    
    /**
     * Flag indicating whether this context has been properly initialized.
     * Used to prevent context override after initial setup.
     */
    private final boolean isSet;
    
    /**
     * Tokenizer instance for processing protected value markers in string operations.
     */
    private final Tokenizer tokenizer;

    /**
     * Private constructor to create a new protection context.
     * 
     * @param canAccess whether protected values can be accessed in this context
     */
    private ProtectionContext(boolean canAccess) {
        this.canAccessProtectedValue = canAccess;
        this.isSet = true;
        this.tokenizer = new Tokenizer();
    }

    /**
     * Sets the protection context for the current thread.
     * <p>
     * This method establishes whether protected values can be accessed during Groovy expression
     * evaluation. Once set, the context cannot be overridden for security reasons.
     * </p>
     * 
     * @param canAccess {@code true} if protected values can be accessed, {@code false} otherwise
     * @throws IllegalStateException if a protection context has already been set for this thread
     * 
     * @see #clear()
     * @see #get()
     */
    public static void set(boolean canAccess) {
        ProtectionContext existing = CONTEXT.get();
        if (existing != null && existing.isSet) {
            throw new IllegalStateException("ProtectionContext has already been set for this thread and cannot be overridden");
        }
        CONTEXT.set(new ProtectionContext(canAccess));
    }

    /**
     * Retrieves the protection context for the current thread.
     * 
     * @return the current protection context, or {@code null} if no context has been set
     * 
     * @see #set(boolean)
     * @see #clear()
     */
    public static ProtectionContext get() {
        return CONTEXT.get();
    }

    /**
     * Clears the protection context for the current thread.
     * <p>
     * This method should be called in a finally block to ensure proper cleanup
     * of the thread-local context after expression evaluation is complete.
     * </p>
     * 
     * @see #set(boolean)
     * @see #get()
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Returns whether protected values can be accessed in this context.
     * 
     * @return {@code true} if protected values can be accessed, {@code false} otherwise
     */
    public boolean canAccessProtectedValue() { 
        return canAccessProtectedValue; 
    }

    /**
     * Returns the tokenizer instance for processing protected value markers.
     * <p>
     * This method has package-private visibility and is intended for use by internal
     * expression handling components. The tokenizer is used to process string concatenation
     * operations that involve protected values, ensuring proper obfuscation.
     * </p>
     * 
     * @return the tokenizer instance associated with this context
     */
    Tokenizer tokenizer() { 
        return tokenizer; 
    }
}