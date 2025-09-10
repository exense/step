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

public final class ProtectionContext {
    private static final ThreadLocal<ProtectionContext> CONTEXT = new ThreadLocal<>();

    private final boolean canAccessProtectedValue;
    private final boolean isSet;
    private final Tokenizer tokenizer;

    private ProtectionContext(boolean canAccess) {
        this.canAccessProtectedValue = canAccess;
        this.isSet = true;
        this.tokenizer = new Tokenizer();
    }

    public static void set(boolean canAccess) {
        ProtectionContext existing = CONTEXT.get();
        if (existing != null && existing.isSet) {
            throw new IllegalStateException("ProtectionContext has already been set for this thread and cannot be overridden");
        }
        CONTEXT.set(new ProtectionContext(canAccess));
    }

    public static ProtectionContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    // Getters
    public boolean canAccessProtectedValue() { return canAccessProtectedValue; }

    // package-private access for ProtectedBinding (not exported to Groovy)
    Tokenizer tokenizer() { return tokenizer; }
}