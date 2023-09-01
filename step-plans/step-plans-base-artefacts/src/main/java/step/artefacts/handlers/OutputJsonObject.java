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

import java.util.Map;

/**
 * Equivalent to the {@link JsonObject}, but implementing the Map<String, Object> instead of Map<String, JsonValue>
 * to simplify usage as output object in {@link CallFunctionHandler}
 */
public interface OutputJsonObject extends JsonStructure, Map<String, Object> {

    /**
     * Returns the array value to which the specified name is mapped.
     * This is a convenience method for {@code (JsonArray)get(name)} to
     * get the value.
     *
     * @param name the name whose associated value is to be returned
     * @return the array value to which the specified name is mapped, or
     * {@code null} if this object contains no mapping for the name
     * @throws ClassCastException if the value to which the specified name
     *                            is mapped is not assignable to JsonArray type
     */
    JsonArray getJsonArray(String name);

    /**
     * Returns the object value to which the specified name is mapped.
     * This is a convenience method for {@code (JsonObject)get(name)} to
     * get the value.
     *
     * @param name the name whose associated value is to be returned
     * @return the object value to which the specified name is mapped, or
     * {@code null} if this object contains no mapping for the name
     * @throws ClassCastException if the value to which the specified name
     *                            is mapped is not assignable to JsonObject type
     */
    JsonObject getJsonObject(String name);

    /**
     * Returns the number value to which the specified name is mapped.
     * This is a convenience method for {@code (JsonNumber)get(name)} to
     * get the value.
     *
     * @param name the name whose associated value is to be returned
     * @return the number value to which the specified name is mapped, or
     * {@code null} if this object contains no mapping for the name
     * @throws ClassCastException if the value to which the specified name
     *                            is mapped is not assignable to JsonNumber type
     */
    JsonNumber getJsonNumber(String name);

    /**
     * Returns the string value to which the specified name is mapped.
     * This is a convenience method for {@code (JsonString)get(name)} to
     * get the value.
     *
     * @param name the name whose associated value is to be returned
     * @return the string value to which the specified name is mapped, or
     * {@code null} if this object contains no mapping for the name
     * @throws ClassCastException if the value to which the specified name
     *                            is mapped is not assignable to JsonString type
     */
    JsonString getJsonString(String name);

    /**
     * A convenience method for
     * {@code getJsonString(name).getString()}
     *
     * @param name whose associated value is to be returned as String
     * @return the String value to which the specified name is mapped
     * @throws NullPointerException if the specified name doesn't have any
     *                              mapping
     * @throws ClassCastException   if the value for specified name mapping
     *                              is not assignable to JsonString
     */
    String getString(String name);

    /**
     * Returns the string value of the associated {@code JsonString} mapping
     * for the specified name. If {@code JsonString} is found, then its
     * {@link jakarta.json.JsonString#getString()} is returned. Otherwise,
     * the specified default value is returned.
     *
     * @param name         whose associated value is to be returned as String
     * @param defaultValue a default value to be returned
     * @return the string value of the associated mapping for the name,
     * or the default value
     */
    String getString(String name, String defaultValue);

    /**
     * A convenience method for
     * {@code getJsonNumber(name).intValue()}
     *
     * @param name whose associated value is to be returned as int
     * @return the int value to which the specified name is mapped
     * @throws NullPointerException if the specified name doesn't have any
     *                              mapping
     * @throws ClassCastException   if the value for specified name mapping
     *                              is not assignable to JsonNumber
     */
    int getInt(String name);

    /**
     * Returns the int value of the associated {@code JsonNumber} mapping
     * for the specified name. If {@code JsonNumber} is found, then its
     * {@link jakarta.json.JsonNumber#intValue()} is returned. Otherwise,
     * the specified default value is returned.
     *
     * @param name         whose associated value is to be returned as int
     * @param defaultValue a default value to be returned
     * @return the int value of the associated mapping for the name,
     * or the default value
     */
    int getInt(String name, int defaultValue);

    /**
     * Returns the boolean value of the associated mapping for the specified
     * name. If the associated mapping is JsonValue.TRUE, then returns true.
     * If the associated mapping is JsonValue.FALSE, then returns false.
     *
     * @param name whose associated value is to be returned as boolean
     * @return the boolean value to which the specified name is mapped
     * @throws NullPointerException if the specified name doesn't have any
     *                              mapping
     * @throws ClassCastException   if the value for specified name mapping
     *                              is not assignable to JsonValue.TRUE or JsonValue.FALSE
     */
    boolean getBoolean(String name);

    /**
     * Returns the boolean value of the associated mapping for the specified
     * name. If the associated mapping is JsonValue.TRUE, then returns true.
     * If the associated mapping is JsonValue.FALSE, then returns false.
     * Otherwise, the specified default value is returned.
     *
     * @param name         whose associated value is to be returned as int
     * @param defaultValue a default value to be returned
     * @return the boolean value of the associated mapping for the name,
     * or the default value
     */
    boolean getBoolean(String name, boolean defaultValue);

    /**
     * Returns {@code true} if the associated value for the specified name is
     * {@code JsonValue.NULL}.
     *
     * @param name name whose associated value is checked
     * @return return true if the associated value is {@code JsonValue.NULL},
     * otherwise false
     * @throws NullPointerException if the specified name doesn't have any
     *                              mapping
     */
    boolean isNull(String name);
}
