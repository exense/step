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
package step.core.execution.notices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of {@link ExecutionNoticeType}s. Plugins register their notice types at startup; the
 * registry is then used at read time to resolve persisted notice instances into displayable messages.
 * Insertion order is preserved when listing types.
 */
public class ExecutionNoticeRegistry {

    private final Map<String, ExecutionNoticeType> types = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Registers a notice type.
     *
     * @throws IllegalArgumentException if a type with the same id is already registered (fail-fast)
     */
    public void register(ExecutionNoticeType type) {
        Objects.requireNonNull(type, "The execution notice type must not be null");
        String id = Objects.requireNonNull(type.id(), "The execution notice type id must not be null");
        synchronized (types) {
            if (types.containsKey(id)) {
                throw new IllegalArgumentException("An execution notice type with id '" + id + "' is already registered");
            }
            types.put(id, type);
        }
    }

    /**
     * @return the registered type for the given id, or {@code null} if no such type is registered
     */
    public ExecutionNoticeType get(String id) {
        return types.get(id);
    }

    /**
     * @return all registered types, in registration order
     */
    public List<ExecutionNoticeType> getAll() {
        synchronized (types) {
            return new ArrayList<>(types.values());
        }
    }
}
