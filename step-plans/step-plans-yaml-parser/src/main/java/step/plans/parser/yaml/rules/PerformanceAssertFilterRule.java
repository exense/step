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
package step.plans.parser.yaml.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.Filter;
import step.artefacts.FilterType;
import step.artefacts.PerformanceAssert;
import step.core.accessors.AbstractOrganizableObject;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * In Yaml format for 'PerformanceAssert' we use dynamic string (measurementName) instead of 'filters'
 */
public class PerformanceAssertFilterRule implements ArtefactFieldConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
         return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
             if (isFiltersFieldInPerformanceAssert(objectClass, field)) {
                 JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                 YamlJsonSchemaHelper.addRef(nestedPropertyParamsBuilder, YamlJsonSchemaHelper.SMART_DYNAMIC_VALUE_STRING_DEF);
                 propertiesBuilder.add(YamlPlanFields.PERFORMANCE_ASSERT_FILTERS_YAML_FIELD, nestedPropertyParamsBuilder);
                 return true;
             }
             return false;
         };
    }

    private static boolean isFiltersFieldInPerformanceAssert(Class<?> objectClass, Field field) {
        return PerformanceAssert.class.isAssignableFrom(objectClass) && field.getName().equals(YamlPlanFields.PERFORMANCE_ASSERT_FILTERS_ORIGINAL_FIELD);
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return new YamlArtefactFieldDeserializationProcessor() {
            @Override
            public boolean deserializeArtefactField(String artefactClass, Map.Entry<String, JsonNode> field, ObjectNode output, ObjectCodec codec) throws JsonProcessingException {
                if(artefactClass.equals("PerformanceAssert") && field.getKey().equals(YamlPlanFields.PERFORMANCE_ASSERT_FILTERS_YAML_FIELD)){
                    ArrayNode filters = createArrayNode(codec);
                    ObjectNode filter = createObjectNode(codec);

                    // fill filter like in EnterpriseDescriptionStepParser
                    filter.set("filter", field.getValue());
                    filter.put("field", AbstractOrganizableObject.NAME);
                    filter.put("filterType", FilterType.REGEX.name());
                    filters.add(filter);
                    output.set(YamlPlanFields.PERFORMANCE_ASSERT_FILTERS_ORIGINAL_FIELD, filters);
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (isFiltersFieldInPerformanceAssert(artefact.getClass(), field)) {
                List<Filter> filters = (List<Filter>) field.get(artefact);
                if (filters != null && !filters.isEmpty()) {
                    if (filters.size() > 1) {
                        throw new IllegalArgumentException("Multiple filters in " + artefact.getClass().getSimpleName() + " are not supported in yaml format");
                    }
                    Filter filter = filters.get(0);
                    if (filter.getFilterType() != FilterType.REGEX) {
                        throw new IllegalArgumentException("Filter type " + filter.getFilterType() + " in " + artefact.getClass().getSimpleName() + " is not supported in yaml format");
                    }
                    if(!filter.getField().get().equals(AbstractOrganizableObject.NAME)){
                        throw new IllegalArgumentException("Filter field " + filter.getField() + " is not supported in yaml format.");
                    }
                    gen.writeObjectField(YamlPlanFields.PERFORMANCE_ASSERT_FILTERS_YAML_FIELD, filter.getFilter());
                }
                return true;
            } else {
                return false;
            }
        };
    }
}
