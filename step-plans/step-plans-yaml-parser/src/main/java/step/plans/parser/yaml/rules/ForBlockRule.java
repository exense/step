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

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.ForBlock;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolConfiguration;
import step.datapool.DataPoolFactory;
import step.datapool.sequence.IntSequenceDataPool;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.schema.YamlDynamicValueJsonSchemaHelper;
import step.plans.parser.yaml.schema.YamlPlanJsonSchemaGenerator;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

public class ForBlockRule implements ArtefactFieldConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (ForBlock.class.isAssignableFrom(objectClass)) {
                if (field.getName().equals(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_TYPE_ORIGINAL_FIELD)) {
                    // don't use 'dataSourceType' field in yaml
                    return true;
                } else if (field.getName().equals(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD)) {
                    // data source parameters are represented as several independent fields in yaml
                    propertiesBuilder.add(YamlPlanFields.FOR_BLOCK_DATA_START_YAML_FIELD, createDynamicNumReference(jsonProvider));
                    propertiesBuilder.add(YamlPlanFields.FOR_BLOCK_DATA_END_YAML_FIELD, createDynamicNumReference(jsonProvider));
                    propertiesBuilder.add(YamlPlanFields.FOR_BLOCK_DATA_INC_YAML_FIELD, createDynamicNumReference(jsonProvider));
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
                prepareOutputDataSourceSectionIfMissing(output, codec);

                switch (field.getKey()) {
                    case YamlPlanFields.FOR_BLOCK_DATA_INC_YAML_FIELD: {
                        ObjectNode jsonNode = (ObjectNode) output.get(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD);
                        jsonNode.set(YamlPlanFields.FOR_BLOCK_DATA_INC_ORIGINAL_FIELD, field.getValue());
                        return true;
                    }
                    case YamlPlanFields.FOR_BLOCK_DATA_END_ORIGINAL_FIELD: {
                        ObjectNode jsonNode = (ObjectNode) output.get(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD);
                        jsonNode.set(YamlPlanFields.FOR_BLOCK_DATA_END_ORIGINAL_FIELD, field.getValue());
                        return true;
                    }
                    case YamlPlanFields.FOR_BLOCK_DATA_START_ORIGINAL_FIELD: {
                        ObjectNode jsonNode = (ObjectNode) output.get(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD);
                        jsonNode.set(YamlPlanFields.FOR_BLOCK_DATA_START_ORIGINAL_FIELD, field.getValue());
                        return true;
                    }
                }
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (ForBlock.class.isAssignableFrom(artefact.getClass())) {
                if (field.getName().equals(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD)) {
                    IntSequenceDataPool dataSource = (IntSequenceDataPool) field.get(artefact);

                    DynamicValue<Integer> startDyn = dataSource.getStart();
                    gen.writeFieldName(YamlPlanFields.FOR_BLOCK_DATA_START_YAML_FIELD);
                    gen.writeObject(startDyn);

                    DynamicValue<Integer> endDyn = dataSource.getEnd();
                    gen.writeFieldName(YamlPlanFields.FOR_BLOCK_DATA_END_YAML_FIELD);
                    gen.writeObject(endDyn);

                    DynamicValue<Integer> incDyn = dataSource.getInc();
                    gen.writeFieldName(YamlPlanFields.FOR_BLOCK_DATA_INC_YAML_FIELD);
                    gen.writeObject(incDyn);
                    return true;
                } else if (field.getName().equals(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_TYPE_ORIGINAL_FIELD)) {
                    return true;
                }
            }
            return false;
        };
    }

    private void prepareOutputDataSourceSectionIfMissing(ObjectNode output, ObjectCodec codec) {
        if (!output.has(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD)) {
            output.put(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_TYPE_ORIGINAL_FIELD, ForBlock.DATA_SOURCE_TYPE);

            DataPoolConfiguration defaultDataPoolConfiguration = DataPoolFactory.getDefaultDataPoolConfiguration(ForBlock.DATA_SOURCE_TYPE);

            output.set(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD, getDefaultObjectMapper().valueToTree(defaultDataPoolConfiguration));
        }
    }

    private static ObjectMapper getDefaultObjectMapper() {
        return DefaultJacksonMapperProvider.getObjectMapper();
    }

    private static JsonObjectBuilder createDynamicNumReference(JsonProvider jsonProvider) {
        JsonObjectBuilder dynamicNumRef = jsonProvider.createObjectBuilder();
        YamlPlanJsonSchemaGenerator.addRef(dynamicNumRef, YamlDynamicValueJsonSchemaHelper.SMART_DYNAMIC_VALUE_NUM_DEF);
        return dynamicNumRef;
    }
}
