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
package step.core.yaml;

import step.core.accessors.AbstractIdentifiableObject;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The free-form {@code metadata} that can be declared on the automation package itself and on each of its entities
 * (keywords, plans, parameters, schedules...). The YAML content is kept as it is, as a map, and persisted in the
 * {@link AbstractIdentifiableObject#getMetadata()} of the business object.
 */
public class YamlMetadata {

    /**
     * The name of the field in YAML and in the business object
     */
    public static final String METADATA_FIELD = "metadata";

    public static final String METADATA_DEF = "MetadataDef";
    public static final String METADATA_VALUE_DEF = "MetadataValueDef";

    /**
     * The keys become the keys of the metadata document stored with the entity, where a dot is interpreted as a path
     * separator and a leading dollar sign as an operator
     */
    public static final String KEY_PATTERN = "^[^.$][^.]*$";
    private static final Pattern KEY_REGEX = Pattern.compile(KEY_PATTERN);

    /**
     * Validates the metadata and sets it on the target. Nothing is set for null or empty metadata.
     *
     * @throws IllegalArgumentException if one of the keys (at any depth) is invalid
     */
    public static void applyTo(AbstractIdentifiableObject target, Map<String, Object> metadata) {
        if (metadata != null && !metadata.isEmpty()) {
            validate(metadata);
            target.setMetadata(metadata);
        }
    }

    /**
     * @return the metadata of the source or null if there is none
     */
    public static Map<String, Object> extractFrom(AbstractIdentifiableObject source) {
        return source.getMetadata();
    }

    /**
     * @throws IllegalArgumentException if one of the keys (at any depth) is invalid
     */
    private static void validate(Map<String, Object> metadata) {
        validateValue(metadata, METADATA_FIELD);
    }

    private static void validateValue(Object value, String path) {
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!KEY_REGEX.matcher(key).matches()) {
                    throw new IllegalArgumentException("Invalid key '" + key + "' in " + path + ": keys must not be empty, " +
                        "contain dots or start with a dollar sign");
                }
                validateValue(entry.getValue(), path + "." + key);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                validateValue(list.get(i), path + "[" + i + "]");
            }
        }
    }
}
