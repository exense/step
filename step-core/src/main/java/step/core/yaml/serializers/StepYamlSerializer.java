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
package step.core.yaml.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.yaml.model.AbstractYamlArtefact;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class StepYamlSerializer<T> extends JsonSerializer<T> {

    protected ObjectMapper yamlObjectMapper;

    public StepYamlSerializer() {
    }

    public StepYamlSerializer(ObjectMapper yamlObjectMapper){
        this.yamlObjectMapper = yamlObjectMapper;
    }

    protected static void removeDefaultValues(ObjectNode actualJson, ObjectNode defaultJson) {
        Set<String> specialFields = Set.of(AbstractYamlArtefact.CHILDREN_FIELD_NAME);
        List<String> fieldsForRemoval = new ArrayList<>();
        actualJson.fieldNames().forEachRemaining(s -> {
            if (!specialFields.contains(s)) {
                JsonNode defaultValue = defaultJson.get(s);
                if (Objects.equals(defaultValue, actualJson.get(s))) {
                    fieldsForRemoval.add(s);
                }
            }
        });
        for (String s : fieldsForRemoval) {
            actualJson.remove(s);
        }
    }

}
