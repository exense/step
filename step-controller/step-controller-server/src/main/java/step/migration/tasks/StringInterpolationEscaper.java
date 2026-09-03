/*
 * Copyright (C) 2026, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */
package step.migration.tasks;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.InterpolatedString;
import step.core.dynamicbeans.NoStringInterpolation;

/**
 * Escapes the plain values of a document authored before the string interpolation existed, so that they keep being
 * used literally. See {@link InterpolatedString#escape(String)} for the escaping itself.
 * <p>
 * The document is walked generically: any nested map shaped like a serialized {@link DynamicValue} holding a plain
 * string value is escaped, wherever it sits in the structure. This is used by both the database migration and the
 * YAML migration, which operate on untyped documents.
 * <p>
 * The values of the fields listed in {@link #CONTAINER_FIELDS} are not escaped themselves. They hold a JSON document
 * (keyword inputs, selection criteria) which isn't interpolated as a whole, see {@link NoStringInterpolation}. Their
 * content is parsed and the plain values it contains are escaped instead.
 */
public class StringInterpolationEscaper {

    /**
     * The fields holding a JSON document rather than user facing text, as of the schema this migration applies to.
     * This set is intentionally frozen: it describes the model at the time the values being migrated were authored,
     * and must not be updated when new container fields are introduced later.
     */
    private static final Set<String> CONTAINER_FIELDS = Set.of(
        "argument",             // CallFunction, the keyword inputs
        "function",             // CallFunction, the keyword selection criteria
        "token",                // TokenSelector, the agent selection criteria
        "input",                // CallPlan, the plan inputs
        "selectionAttributes",  // CallPlan, the plan selection criteria
        "output"                // Return, the output of a composite keyword
    );

    private static final String DYNAMIC_FIELD = "dynamic";
    private static final String VALUE_FIELD = "value";
    private static final String EXPRESSION_FIELD = "expression";
    private static final String EXPRESSION_TYPE_FIELD = "expressionType";

    private static final Set<String> DYNAMIC_VALUE_FIELDS = Set.of(DYNAMIC_FIELD, VALUE_FIELD, EXPRESSION_FIELD, EXPRESSION_TYPE_FIELD);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private StringInterpolationEscaper() {
    }

    /**
     * Escapes all the plain values contained in the provided document, recursively
     *
     * @param document the document to be migrated, modified in place
     * @return true if any value was actually modified
     */
    public static boolean escapeDocument(Map<String, Object> document) {
        return escapeMap(document, false);
    }

    private static boolean escapeMap(Map<String, Object> map, boolean withinContainer) {
        boolean modified = false;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> childMap = (Map<String, Object>) value;
                if (isPlainDynamicValue(childMap)) {
                    // Within a container the fields are the input names, not model fields, so the exclusion of the
                    // container fields must not apply to them
                    boolean isContainer = !withinContainer && CONTAINER_FIELDS.contains(key);
                    modified |= escapeDynamicValue(childMap, isContainer);
                } else {
                    modified |= escapeMap(childMap, withinContainer);
                }
            } else if (value instanceof List) {
                modified |= escapeList((List<?>) value, withinContainer);
            }
        }
        return modified;
    }

    private static boolean escapeList(List<?> list, boolean withinContainer) {
        boolean modified = false;
        for (Object element : list) {
            if (element instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> childMap = (Map<String, Object>) element;
                modified |= escapeMap(childMap, withinContainer);
            } else if (element instanceof List) {
                modified |= escapeList((List<?>) element, withinContainer);
            }
        }
        return modified;
    }

    /**
     * @param map the serialized dynamic value to be escaped
     * @param isContainer whether the value holds a JSON document instead of user facing text
     */
    private static boolean escapeDynamicValue(Map<String, Object> map, boolean isContainer) {
        String value = (String) map.get(VALUE_FIELD);
        if (isContainer) {
            String escapedDocument = escapeNestedDocument(value);
            if (escapedDocument != null) {
                map.put(VALUE_FIELD, escapedDocument);
                return true;
            }
            return false;
        }
        String escaped = InterpolatedString.escape(value);
        if (!escaped.equals(value)) {
            map.put(VALUE_FIELD, escaped);
            return true;
        }
        return false;
    }

    /**
     * Escapes the plain values contained in a JSON document held by a container field
     *
     * @return the escaped document, or null if it isn't a JSON object or if nothing had to be escaped
     */
    private static String escapeNestedDocument(String json) {
        if (json == null || json.isBlank() || !json.contains(InterpolatedString.EXPRESSION_PREFIX)) {
            return null;
        }
        Map<String, Object> parsed;
        try {
            parsed = OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            // Not a JSON object. The value is left untouched: it isn't interpolated as a whole either
            return null;
        }
        if (!escapeMap(parsed, true)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(parsed);
        } catch (Exception e) {
            throw new RuntimeException("Error while rewriting the JSON document " + json, e);
        }
    }

    /**
     * @return true if the provided map is a serialized {@link DynamicValue} holding a plain string value. Values
     * defined as expressions aren't concerned: they were already evaluated as groovy before the interpolation existed
     */
    private static boolean isPlainDynamicValue(Map<String, Object> map) {
        if (!(map.get(VALUE_FIELD) instanceof String) || !DYNAMIC_VALUE_FIELDS.containsAll(map.keySet())) {
            return false;
        }
        Object dynamic = map.get(DYNAMIC_FIELD);
        return !Boolean.TRUE.equals(dynamic);
    }
}
