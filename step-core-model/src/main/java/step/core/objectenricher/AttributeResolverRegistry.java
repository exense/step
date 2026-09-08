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
package step.core.objectenricher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Resolves symbolic attribute values written as {@code @<value>} into the value to be persisted.
 * <p>
 * Used for attributes an administrator writes by hand, outside of any project context: the embedded
 * automation package sidecars name their project as {@code "@Common"} while the persisted attribute
 * is the project id. Enterprise registers the resolver for {@code project}; without it the values
 * are left untouched.
 */
public class AttributeResolverRegistry {

    private static final String RESOLVABLE_PREFIX = "@";

    private final Map<String, Function<String, String>> resolvers = new ConcurrentHashMap<>();

    public void register(String attributeKey, Function<String, String> resolver) {
        resolvers.put(attributeKey, resolver);
    }

    /**
     * @return the resolved value, or the value unchanged when it carries no prefix or when no
     * resolver is registered for the key
     */
    public String resolve(String key, String value) {
        Function<String, String> resolver = resolvers.get(key);
        String resolved;
        if (resolver != null && value != null && value.startsWith(RESOLVABLE_PREFIX)) {
            resolved = resolver.apply(value.substring(RESOLVABLE_PREFIX.length()));
        } else {
            resolved = value;
        }
        return resolved;
    }

    public Map<String, String> resolveAll(Map<String, String> attributes) {
        Map<String, String> resolved = new LinkedHashMap<>();
        if (attributes != null) {
            attributes.forEach((key, value) -> resolved.put(key, resolve(key, value)));
        }
        return resolved;
    }
}
