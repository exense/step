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

import jakarta.json.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class UserFriendlyJsonObject implements JsonObject {

    private JsonObject wrapped;

    private Map<String, JsonValue> extendedMap;

    public UserFriendlyJsonObject(JsonObject wrapped) {
        this.wrapped = wrapped;
        // TODO: probably it is better to generate extended map on the fly
        this.extendedMap = new HashMap<>(wrapped);
        this.extendedMap.put("$", prepareValues());
    }

    public UserFriendlyJsonValues prepareValues(){
        Map<String, Object> simpleValues = new HashMap<>();
        for (Entry<String, JsonValue> nestedField : wrapped.entrySet()) {
            if(nestedField.getValue().getValueType() == ValueType.STRING){
                simpleValues.put(nestedField.getKey(), wrapped.getJsonString(nestedField.getKey()).getString());
            }
            // TODO: same conversion for another types and for nested objects
        }
        return new UserFriendlyJsonValues(simpleValues);
    }

    @Override
    public JsonValue getValue(String jsonPointer) {
        return wrapped.getValue(jsonPointer);
    }

    @Override
    public ValueType getValueType() {
        return wrapped.getValueType();
    }

    @Override
    public JsonObject asJsonObject() {
        return wrapped.asJsonObject();
    }

    @Override
    public JsonArray asJsonArray() {
        return wrapped.asJsonArray();
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }

    @Override
    public JsonArray getJsonArray(String name) {
        return wrapped.getJsonArray(name);
    }

    @Override
    public JsonObject getJsonObject(String name) {
        return wrapped.getJsonObject(name);
    }

    @Override
    public JsonNumber getJsonNumber(String name) {
        return wrapped.getJsonNumber(name);
    }

    @Override
    public JsonString getJsonString(String name) {
        return wrapped.getJsonString(name);
    }

    @Override
    public String getString(String name) {
        return wrapped.getString(name);
    }

    @Override
    public String getString(String name, String defaultValue) {
        return wrapped.getString(name, defaultValue);
    }

    @Override
    public int getInt(String name) {
        return wrapped.getInt(name);
    }

    @Override
    public int getInt(String name, int defaultValue) {
        return wrapped.getInt(name, defaultValue);
    }

    @Override
    public boolean getBoolean(String name) {
        return wrapped.getBoolean(name);
    }

    @Override
    public boolean getBoolean(String name, boolean defaultValue) {
        return wrapped.getBoolean(name, defaultValue);
    }

    @Override
    public boolean isNull(String name) {
        return wrapped.isNull(name);
    }

    @Override
    public int size() {
        return extendedMap.size();
    }

    @Override
    public boolean isEmpty() {
        return extendedMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return extendedMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return extendedMap.containsValue(value);
    }

    @Override
    public JsonValue get(Object key) {
        return extendedMap.get(key);
    }

    @Override
    public JsonValue put(String key, JsonValue value) {
        return extendedMap.put(key, value);
    }

    @Override
    public JsonValue remove(Object key) {
        return extendedMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends JsonValue> m) {
        extendedMap.putAll(m);
    }

    @Override
    public void clear() {
        extendedMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return extendedMap.keySet();
    }

    @Override
    public Collection<JsonValue> values() {
        return extendedMap.values();
    }

    @Override
    public Set<Entry<String, JsonValue>> entrySet() {
        return extendedMap.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return extendedMap.equals(o);
    }

    @Override
    public int hashCode() {
        return extendedMap.hashCode();
    }

    @Override
    public JsonValue getOrDefault(Object key, JsonValue defaultValue) {
        return extendedMap.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super JsonValue> action) {
        extendedMap.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super JsonValue, ? extends JsonValue> function) {
        extendedMap.replaceAll(function);
    }

    @Override
    public JsonValue putIfAbsent(String key, JsonValue value) {
        return extendedMap.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return extendedMap.remove(key, value);
    }

    @Override
    public boolean replace(String key, JsonValue oldValue, JsonValue newValue) {
        return extendedMap.replace(key, oldValue, newValue);
    }

    @Override
    public JsonValue replace(String key, JsonValue value) {
        return extendedMap.replace(key, value);
    }

    @Override
    public JsonValue computeIfAbsent(String key, Function<? super String, ? extends JsonValue> mappingFunction) {
        return extendedMap.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public JsonValue computeIfPresent(String key, BiFunction<? super String, ? super JsonValue, ? extends JsonValue> remappingFunction) {
        return extendedMap.computeIfPresent(key, remappingFunction);
    }

    @Override
    public JsonValue compute(String key, BiFunction<? super String, ? super JsonValue, ? extends JsonValue> remappingFunction) {
        return extendedMap.compute(key, remappingFunction);
    }

    @Override
    public JsonValue merge(String key, JsonValue value, BiFunction<? super JsonValue, ? super JsonValue, ? extends JsonValue> remappingFunction) {
        return extendedMap.merge(key, value, remappingFunction);
    }
}
