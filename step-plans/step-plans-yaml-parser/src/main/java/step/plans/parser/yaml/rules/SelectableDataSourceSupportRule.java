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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.AbstractForBlock;
import step.artefacts.ForEachBlock;
import step.datapool.DataPoolConfiguration;
import step.datapool.DataPoolFactory;
import step.datapool.DataSources;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class SelectableDataSourceSupportRule implements ArtefactFieldConversionRule {
    protected final ObjectMapper stepYamlMapper;
    private final DataSourceFieldsYamlHelper dataSourceFieldsYamlHelper = new DataSourceFieldsYamlHelper();

    public SelectableDataSourceSupportRule(ObjectMapper stepYamlMapper) {
        // TODO: temporary solution (we need to have the yaml object mapper configured with some serializers like step.plans.parser.yaml.serializers.YamlDynamicValueSerializer to process nested data source objects
        // in fact this mapper is only required for getArtefactFieldDeserializationProcessor()
        this.stepYamlMapper = stepYamlMapper;
    }

    private static List<String> getAllDataSourceTypes() {
        return List.of(
                DataSources.SEQUENCE,
                DataSources.CSV,
                DataSources.EXCEL,
                DataSources.JSON,
                DataSources.FOLDER,
                DataSources.GSHEET,
                DataSources.JSON_ARRAY,
                DataSources.SQL,
                DataSources.FILE
        );
    }

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (applicableClass(objectClass)) {
                if (field.getName().equals(YamlPlanFields.DATA_SOURCE_TYPE_ORIGINAL_FIELD)) {
                    // don't use 'dataSourceType' field in yaml
                    return true;
                } else if (field.getName().equals(YamlPlanFields.DATA_SOURCE_ORIGINAL_FIELD)) {
                    JsonArrayBuilder oneOf = jsonProvider.createArrayBuilder();
                    List<String> dataSourceTypes = SelectableDataSourceSupportRule.getAllDataSourceTypes();

                    for (String dataSourceType : dataSourceTypes) {
                        JsonObjectBuilder dataSourceTypeProperty = jsonProvider.createObjectBuilder();

                        JsonObjectBuilder dataSourceProperties = jsonProvider.createObjectBuilder();
                        dataSourceFieldsYamlHelper.fillDataSourceJsonSchemaParams(dataSourceType, jsonProvider, dataSourceProperties, isForWriteEditable(), requiredPropertiesOutput);

                        dataSourceTypeProperty.add(dataSourceType, jsonProvider.createObjectBuilder().add("type", "object").add("additionalProperties", false).add("properties", dataSourceProperties));

                        oneOf.add(jsonProvider.createObjectBuilder().add("type", "object").add("additionalProperties", false).add("properties", dataSourceTypeProperty));
                    }

                    propertiesBuilder.add("dataSource", jsonProvider.createObjectBuilder().add("oneOf", oneOf));

                    return true;
                }
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return (artefactClass, field, output, codec) -> {
            if (applicableArtefactName(artefactClass) && YamlPlanFields.DATA_SOURCE_YAML_FIELD.equals(field.getKey())) {

                if (field.getValue() != null && field.getValue().isContainerNode()) {
                    // the only one element here - object type
                    String dataSourceType = field.getValue().fieldNames().next();
                    dataSourceFieldsYamlHelper.prepareOutputDataSourceSectionIfMissing(output, dataSourceType, stepYamlMapper);

                    JsonNode dataSourceProperties = field.getValue().get(dataSourceType);
                    if (dataSourceProperties != null && dataSourceProperties.isContainerNode()) {

                        DataPoolConfiguration dataPoolConfiguration = DataPoolFactory.getDefaultDataPoolConfiguration(dataSourceType);
                        List<PropertyDescriptor> fileReferences = dataSourceFieldsYamlHelper.getResourceReferencePropertyDescriptors(dataPoolConfiguration.getClass());

                        Iterator<Map.Entry<String, JsonNode>> fields = dataSourceProperties.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> next = fields.next();
                            boolean isFileReference = false;
                            for (PropertyDescriptor fileReference : fileReferences) {
                                if (fileReference.getName().equals(next.getKey())) {
                                    isFileReference = true;
                                    break;
                                }
                            }

                            // move all nested elements to 'dataSource' and for file reference convert the value to 'resourceId:3234824242'
                            ObjectNode dataSourceOutput = (ObjectNode) output.get(YamlPlanFields.DATA_SOURCE_ORIGINAL_FIELD);
                            if (isFileReference) {
                                dataSourceOutput.set(next.getKey(), dataSourceFieldsYamlHelper.convertFileReferenceToTechFormat(next.getValue()));
                            } else {
                                dataSourceOutput.set(next.getKey(), next.getValue());
                            }
                        }
                    }
                }
                return true;
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (applicableClass(artefact.getClass())) {
                if (field.getName().equals(YamlPlanFields.DATA_SOURCE_ORIGINAL_FIELD)) {
                    String dataSourceType = ((AbstractForBlock) artefact).getDataSourceType();
                    gen.writeFieldName(YamlPlanFields.DATA_SOURCE_YAML_FIELD);
                    gen.writeStartObject();
                    gen.writeFieldName(dataSourceType);
                    gen.writeStartObject();
                    dataSourceFieldsYamlHelper.serializeDataSourceFields(artefact, field, gen, isForWriteEditable());
                    gen.writeEndObject();
                    gen.writeEndObject();
                    return true;
                } else if (field.getName().equals(YamlPlanFields.DATA_SOURCE_TYPE_ORIGINAL_FIELD)) {
                    return true;
                }
            }
            return false;
        };
    }

    protected abstract boolean applicableClass(Class<?> artefactClass);

    protected abstract boolean applicableArtefactName(String artefactClass);

    protected abstract boolean isForWriteEditable();
}
