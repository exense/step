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

import java.util.*;

public class UserFriendlyJsonObject implements OutputJsonObject {

    private final JsonObject wrapped;

    private final Map<String, Object> unwrappedValues;

    public UserFriendlyJsonObject(JsonObject wrapped) {
        this.wrapped = wrapped;
        this.unwrappedValues = unwrapJsonObject(wrapped);
    }

    protected Map<String, Object> unwrapJsonObject(JsonObject jsonObject) {
        Map<String, Object> simpleValues = new HashMap<>();
        for (Entry<String, JsonValue> nestedField : jsonObject.entrySet()) {
            Object simpleValue = unwrapSimpleValue(nestedField.getValue());
            simpleValues.put(nestedField.getKey(), simpleValue);
        }
        return simpleValues;
    }

    private Object unwrapSimpleValue(JsonValue fieldValue) {
        Object simpleValue;

        if (fieldValue.getValueType() == ValueType.OBJECT) {
            simpleValue = unwrapJsonObject(fieldValue.asJsonObject());
        } else if (fieldValue.getValueType() == ValueType.STRING) {
            simpleValue = ((JsonString) fieldValue).getString();
        } else if (fieldValue.getValueType() == ValueType.NULL) {
            simpleValue = null;
        } else if (fieldValue.getValueType() == ValueType.NUMBER) {
            JsonNumber numberValue = (JsonNumber) fieldValue;
            if (numberValue.isIntegral()) {
                simpleValue = numberValue.intValue();
            } else {
                simpleValue = numberValue.doubleValue();
            }
        } else if (fieldValue.getValueType() == ValueType.FALSE) {
            simpleValue = false;
        } else if (fieldValue.getValueType() == ValueType.TRUE) {
            simpleValue = true;
        } else if (fieldValue.getValueType() == ValueType.ARRAY) {
            JsonArray jsonArray = fieldValue.asJsonArray();
            List<Object> list = new ArrayList<>();
            for (JsonValue jsonValue : jsonArray) {
                list.add(unwrapSimpleValue(jsonValue));
            }
            simpleValue = list;
        } else {
            throw new UnsupportedOperationException(fieldValue.getValueType() + " is not supported");
        }
        return simpleValue;
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
        return unwrappedValues.size();
    }

    @Override
    public boolean isEmpty() {
        return unwrappedValues.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return unwrappedValues.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return unwrappedValues.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return unwrappedValues.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return unwrappedValues.keySet();
    }

    @Override
    public Collection<Object> values() {
        return unwrappedValues.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return unwrappedValues.entrySet();
    }

}
