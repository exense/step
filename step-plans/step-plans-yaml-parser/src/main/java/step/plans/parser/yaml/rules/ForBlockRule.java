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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.spi.JsonProvider;
import step.artefacts.ForBlock;
import step.datapool.DataSources;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.util.Map;

public class ForBlockRule implements ArtefactFieldConversionRule {

    private final DataSourceFieldsYamlHelper dataSourceFieldsYamlHelper = new DataSourceFieldsYamlHelper();

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (ForBlock.class.isAssignableFrom(objectClass)) {
                if (field.getName().equals(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_TYPE_ORIGINAL_FIELD)) {
                    // don't use 'dataSourceType' field in yaml
                    return true;
                } else if (field.getName().equals(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD)) {
                    // data source parameters are represented as several independent fields in yaml
                    dataSourceFieldsYamlHelper.fillDataSourceJsonSchemaParams(ForBlock.DATA_SOURCE_TYPE, jsonProvider, propertiesBuilder);
                    return true;
                }
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return (artefactClass, field, output, codec) -> {
            if (artefactClass.equals(ForBlock.FOR_BLOCK_ARTIFACT_NAME)) {
                dataSourceFieldsYamlHelper.prepareOutputDataSourceSectionIfMissing(output, ForBlock.DATA_SOURCE_TYPE);
                return deserializeDataSourceField(field, output, ForBlock.DATA_SOURCE_TYPE);
            }
            return false;
        };
    }

    public boolean deserializeDataSourceField(Map.Entry<String, JsonNode> field, ObjectNode output, String dataSourceType) {
        // move all datasource properties to upper level
        if (dataSourceType.equals(DataSources.SEQUENCE)) {
            switch (field.getKey()) {
                case YamlPlanFields.FOR_BLOCK_DATA_INC_YAML_FIELD: {
                    ObjectNode jsonNode = (ObjectNode) output.get(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD);
                    jsonNode.set(YamlPlanFields.FOR_BLOCK_DATA_INC_ORIGINAL_FIELD, field.getValue());
                    return true;
                }
                case YamlPlanFields.FOR_BLOCK_DATA_END_YAML_FIELD: {
                    ObjectNode jsonNode = (ObjectNode) output.get(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD);
                    jsonNode.set(YamlPlanFields.FOR_BLOCK_DATA_END_ORIGINAL_FIELD, field.getValue());
                    return true;
                }
                case YamlPlanFields.FOR_BLOCK_DATA_START_YAML_FIELD: {
                    ObjectNode jsonNode = (ObjectNode) output.get(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD);
                    jsonNode.set(YamlPlanFields.FOR_BLOCK_DATA_START_ORIGINAL_FIELD, field.getValue());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (ForBlock.class.isAssignableFrom(artefact.getClass())) {
                if (field.getName().equals(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD)) {
                    dataSourceFieldsYamlHelper.serializeDataSourceFields(artefact, field, gen);
                    return true;
                } else if (field.getName().equals(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_TYPE_ORIGINAL_FIELD)) {
                    return true;
                }
            }
            return false;
        };
    }

}
