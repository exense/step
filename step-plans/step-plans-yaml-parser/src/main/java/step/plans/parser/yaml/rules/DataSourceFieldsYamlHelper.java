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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.attachments.FileResolver;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.datapool.DataPoolConfiguration;
import step.datapool.DataPoolFactory;
import step.handlers.javahandler.jsonschema.*;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.schema.AggregatedJsonSchemaFieldProcessor;
import step.plans.parser.yaml.schema.YamlPlanJsonSchemaGenerator;
import step.plans.parser.yaml.schema.YamlResourceReferenceJsonSchemaHelper;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class DataSourceFieldsYamlHelper {

    private static final ObjectMapper defaultObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

    public DataSourceFieldsYamlHelper() {
    }

    public void fillDataSourceJsonSchemaParams(String dataSourceType, JsonProvider jsonProvider, JsonObjectBuilder propertiesBuilder) throws JsonSchemaPreparationException {
        List<JsonSchemaFieldProcessor> fieldProcessors = new ArrayList<>();

        // -- PROCESSING RULES
        fieldProcessors.add(new CommonFilteredFieldRule().getJsonSchemaFieldProcessor(jsonProvider));

        // skip technical fields from
        fieldProcessors.add((aClass, field, fieldMetadata, jsonObjectBuilder, list) -> {
            if (DataPoolConfiguration.class.isAssignableFrom(aClass)) {
                return isTechnicalDataPoolField(field);
            }
            return false;
        });

        fieldProcessors.add((aClass, field, fieldMetadata, jsonObjectBuilder, list) -> {
            PropertyDescriptor descriptor = getResourceReferencePropertyDescriptors(aClass).stream().filter(pd -> pd.getName().equals(field.getName())).findFirst().orElse(null);
            if (descriptor != null) {
                jsonObjectBuilder.add(
                        fieldMetadata.getFieldName(),
                        YamlPlanJsonSchemaGenerator.addRef(jsonProvider.createObjectBuilder(), YamlResourceReferenceJsonSchemaHelper.RESOURCE_REFERENCE_DEF)
                );
                return true;
            }
            return false;
        });

        fieldProcessors.add(new DynamicFieldRule().getJsonSchemaFieldProcessor(jsonProvider));
        fieldProcessors.add(new EnumFieldRule().getJsonSchemaFieldProcessor(jsonProvider));

        JsonSchemaCreator jsonSchemaCreator = new JsonSchemaCreator(
                jsonProvider,
                new AggregatedJsonSchemaFieldProcessor(fieldProcessors),
                new DefaultFieldMetadataExtractor()
        );

        DataPoolConfiguration config = DataPoolFactory.getDefaultDataPoolConfiguration(dataSourceType);

        jsonSchemaCreator.processFields(config.getClass(), propertiesBuilder, getAllFieldsAccessible(config), new ArrayList<>());
    }

    public boolean isTechnicalDataPoolField(Field field) {
        Set<String> technicalFields = Set.of("forWrite");
        return technicalFields.contains(field.getName());
    }

    public List<PropertyDescriptor> getResourceReferencePropertyDescriptors(Class<?> aClass) {
        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(aClass).getPropertyDescriptors();
            List<PropertyDescriptor> entityReferenceDescriptors = new ArrayList<>();
            for (PropertyDescriptor pd : propertyDescriptors) {
                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    EntityReference entityReference = readMethod.getAnnotation(EntityReference.class);
                    if (entityReference != null && EntityManager.resources.equals(entityReference.type())) {
                        entityReferenceDescriptors.add(pd);
                    }
                }
            }
            return entityReferenceDescriptors;
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Field> getAllFieldsAccessible(DataPoolConfiguration config) {
        List<Field> allFieldsInHierarchy = new ArrayList<>();
        Class<?> currentClass = config.getClass();
        while (currentClass != null) {
            allFieldsInHierarchy.addAll(List.of(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        for (Field field : allFieldsInHierarchy) {
            field.setAccessible(true);
        }
        Collections.reverse(allFieldsInHierarchy);
        return allFieldsInHierarchy;
    }

    public JsonNode convertFileReferenceToTechFormat(JsonNode fileReferenceField) {
        if (!fileReferenceField.isContainerNode()) {
            // TODO: in automation packages we have to support relative paths to the files included in package and search for resources by name e.t.c
            throw new IllegalArgumentException("Only file references by ID are supported now");
        }
        JsonNode resourceId = fileReferenceField.get(YamlPlanFields.FILE_REFERENCE_RESOURCE_ID_FIELD);
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource id should be defined for the file reference");
        }
        return new TextNode(FileResolver.RESOURCE_PREFIX + resourceId.asText());
    }

    public void prepareOutputDataSourceSectionIfMissing(ObjectNode output, String dataSourceType) {
        if (!output.has(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD)) {
            output.put(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_TYPE_ORIGINAL_FIELD, dataSourceType);

            DataPoolConfiguration defaultDataPoolConfiguration = DataPoolFactory.getDefaultDataPoolConfiguration(dataSourceType);

            output.set(YamlPlanFields.FOR_BLOCK_DATA_SOURCE_ORIGINAL_FIELD, defaultObjectMapper.valueToTree(defaultDataPoolConfiguration));
        }
    }

    public void serializeDataSourceFields(AbstractArtefact artefact, Field dataSourceField, JsonGenerator gen) throws IllegalAccessException, IOException {
        DataPoolConfiguration dataSource = (DataPoolConfiguration) dataSourceField.get(artefact);
        List<Field> allFields = getAllFieldsAccessible(dataSource);
        List<PropertyDescriptor> resourceReferencePropertyDescriptors = getResourceReferencePropertyDescriptors(dataSource.getClass());
        for (Field field : allFields) {
            if (isTechnicalDataPoolField(field)) {
                continue;
            }

            boolean isResourceReference = false;
            for (PropertyDescriptor pd : resourceReferencePropertyDescriptors) {
                if (field.getName().equals(pd.getName())) {
                    isResourceReference = true;
                    break;
                }
            }

            if (!isResourceReference) {
                // just serialize the value
                gen.writeFieldName(field.getName());
                gen.writeObject(field.get(dataSource));
            } else {
                // file reference is a dynamic string in java class
                // here we extract the resource id from dynamic value and store this resource id in yaml
                DynamicValue<String> value = (DynamicValue<String>) field.get(dataSource);
                if (value != null && !value.getValue().isEmpty()) {
                    gen.writeFieldName(field.getName());
                    gen.writeStartObject();
                    gen.writeFieldName(YamlPlanFields.FILE_REFERENCE_RESOURCE_ID_FIELD);
                    gen.writeString(value.getValue().replaceFirst(FileResolver.RESOURCE_PREFIX, ""));
                    gen.writeEndObject();
                }
            }
        }
    }


}
