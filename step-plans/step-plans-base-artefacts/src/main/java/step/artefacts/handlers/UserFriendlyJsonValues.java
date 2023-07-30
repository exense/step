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
package step.artefacts.handlers;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class UserFriendlyJsonValues implements Map<String, Object>, JsonValue {

    private Map<String, Object> simpleValues;

    public UserFriendlyJsonValues(Map<String, Object> simpleValues) {
        this.simpleValues = simpleValues;
    }

    @Override
    public int size() {
        return simpleValues.size();
    }

    @Override
    public boolean isEmpty() {
        return simpleValues.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return simpleValues.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return simpleValues.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return simpleValues.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return simpleValues.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return simpleValues.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        simpleValues.putAll(m);
    }

    @Override
    public void clear() {
        simpleValues.clear();
    }

    @Override
    public Set<String> keySet() {
        return simpleValues.keySet();
    }

    @Override
    public Collection<Object> values() {
        return simpleValues.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return simpleValues.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return simpleValues.equals(o);
    }

    @Override
    public int hashCode() {
        return simpleValues.hashCode();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return simpleValues.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        simpleValues.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        simpleValues.replaceAll(function);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return simpleValues.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return simpleValues.remove(key, value);
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        return simpleValues.replace(key, oldValue, newValue);
    }

    @Override
    public Object replace(String key, Object value) {
        return simpleValues.replace(key, value);
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
        return simpleValues.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return simpleValues.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return simpleValues.compute(key, remappingFunction);
    }

    @Override
    public Object merge(String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return simpleValues.merge(key, value, remappingFunction);
    }

    @Override
    public ValueType getValueType() {
        // fake value type
        return ValueType.NULL;
    }

    @Override
    public JsonObject asJsonObject() {
        return JsonValue.super.asJsonObject();
    }

    @Override
    public JsonArray asJsonArray() {
        return JsonValue.super.asJsonArray();
    }
}
