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
package step.plans.parser.yaml.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.core.yaml.serializers.NamedEntityYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;
import step.handlers.javahandler.jsonschema.FieldMetadataExtractor;
import step.plans.parser.yaml.model.AbstractYamlArtefact;
import step.plans.parser.yaml.model.NamedYamlArtefact;
import step.plans.parser.yaml.schema.YamlPlanJsonSchemaGenerator;

import java.io.IOException;
import java.util.List;

@StepYamlSerializerAddOn(targetClasses = {NamedYamlArtefact.class})
public class NamedYamlArtefactSerializer extends StepYamlSerializer<NamedYamlArtefact> {

    private final List<YamlArtefactFieldSerializationProcessor> customFieldProcessors = null;
    private final FieldMetadataExtractor metadataExtractor = null;
    private ObjectMapper stepYamlMapper;

    public NamedYamlArtefactSerializer() {
        super();
    }

    public NamedYamlArtefactSerializer(ObjectMapper stepYamlMapper) {
        super(stepYamlMapper);
    }

    protected FieldMetadataExtractor prepareMetadataExtractor() {
        return YamlPlanJsonSchemaGenerator.prepareMetadataExtractor();
    }

    /*
    protected List<YamlArtefactFieldSerializationProcessor> prepareFieldProcessors() {
        List<YamlArtefactFieldSerializationProcessor> result =  new ArrayList<>();

        // -- BASIC PROCESSING RULES

        result.add(new TechnicalFieldRule().getArtefactFieldSerializationProcessor());
        result.add(new CommonFilteredFieldRule().getArtefactFieldSerializationProcessor());
        result.add(new NodeNameRule().getArtefactFieldSerializationProcessor());
        result.add((artefact, field, fieldMetadata, gen) -> {
            if (field.getName().equals(YamlPlanFields.ARTEFACT_CHILDREN)) {
                List<AbstractArtefact> children = (List<AbstractArtefact>) field.get(artefact);
                if(children != null && !children.isEmpty()) {
                    gen.writeFieldName(YamlPlanFields.ARTEFACT_CHILDREN);
                    gen.writeStartArray();
                    for (AbstractArtefact child : children) {
                        processArtefact(gen, child);
                    }
                    gen.writeEndArray();
                }
                return true;
            }
            return false;
        });

        result.add((artefact, field, fieldMetadata, gen) -> {
            // skip dynamic fields with empty non-dynamic values
            if (DynamicValue.class.isAssignableFrom(field.getType())) {
                DynamicValue value = (DynamicValue) field.get(artefact);
                return value == null || (!value.isDynamic() && value.getValue() == null);
            }
            return false;
        });

        // -- RULES FROM EXTENSIONS HAVE LESS PRIORITY THAN BASIC RULES, BUT MORE PRIORITY THAN OTHER RULES
        result.addAll(getExtensions());

        // -- RULES FOR OS ARTEFACTS
        // TODO: all these rules for OS artefacts should be taken in the same way (pluggable) via getFieldExtensions
        result.add(new KeywordSelectionRule().getArtefactFieldSerializationProcessor());
        result.add(new KeywordRoutingRule().getArtefactFieldSerializationProcessor());
        result.add(new KeywordInputsRule().getArtefactFieldSerializationProcessor());
        result.add(new FunctionGroupSelectionRule().getArtefactFieldSerializationProcessor());
        result.add(new CheckExpressionRule().getArtefactFieldSerializationProcessor());
        result.add(new ForBlockRule(stepYamlMapper).getArtefactFieldSerializationProcessor());
        result.add(new ForEachBlockRule(stepYamlMapper).getArtefactFieldSerializationProcessor());
        result.add(new DataSetRule(stepYamlMapper).getArtefactFieldSerializationProcessor());
        result.add(new PerformanceAssertFilterRule().getArtefactFieldSerializationProcessor());

        return result;
    }
    */


//    protected List<YamlArtefactFieldSerializationProcessor> getExtensions() {
//        List<YamlArtefactFieldSerializationProcessor> extensions = new ArrayList<>();
//        CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanReaderExtension.LOCATION, YamlPlanReaderExtension.class, Thread.currentThread().getContextClassLoader()).stream()
//                .map(newInstanceAs(YamlPlanReaderExtender.class)).forEach(e -> extensions.addAll(e.getSerializationExtensions()));
//        return extensions;
//    }

    @Override
    public void serialize(NamedYamlArtefact value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        NamedEntityYamlSerializer<AbstractYamlArtefact<?>> ser = new NamedEntityYamlSerializer<>() {
            @Override
            protected String resolveYamlName(Class<AbstractYamlArtefact<?>> clazz) {
                return AutomationPackageNamedEntityUtils.getEntityNameByClass(clazz);
            }
        };
        ser.serialize(value.getAbstractArtefact(), gen, serializers);
    }

    // TODO: cleanup
//    private void processArtefact(JsonGenerator gen, AbstractArtefact artefact) throws IOException, IllegalAccessException {
//        gen.writeStartObject();
//        String artefactName = AbstractArtefact.getArtefactName(artefact.getClass());
//        gen.writeFieldName(YamlPlanFields.javaArtefactNameToYaml(artefactName));
//        gen.writeStartObject();
//        serializeArtefactFields(artefact, gen);
//        gen.writeEndObject();
//        gen.writeEndObject();
//    }


//    protected void serializeArtefactFields(AbstractArtefact value, JsonGenerator gen) {
//        List<Field> allFieldsInArtefact = getAllFieldsInArtefact(value);
//        for (Field field : allFieldsInArtefact) {
//            try {
//                boolean processedAsCustomField = false;
//                for (YamlArtefactFieldSerializationProcessor customFieldProcessor : customFieldProcessors) {
//                    if (customFieldProcessor.serializeArtefactField(value, field, metadataExtractor.extractMetadata(value.getClass(), field), gen)) {
//                        processedAsCustomField = true;
//                        break;
//                    }
//                }
//                try {
//                    if (!processedAsCustomField) {
//                         default processing
//                        Object fieldValue = field.get(value);
//                        if (fieldValue != null) {
//                            gen.writeObjectField(field.getName(), fieldValue);
//                        }
//                    }
//                } catch (IllegalAccessException e) {
//                    throw new RuntimeException("Unable to get field value (" + field.getName() + ")", e);
//                }
//            } catch (Exception ex) {
//                throw new RuntimeException("Unable to serialize field " + field.getName() + " in artifact "
//                        + value.getClass().getSimpleName() + " (" + value.getAttribute(AbstractOrganizableObject.NAME) + ")", ex);
//            }
//        }
//    }

//    private List<Field> getAllFieldsInArtefact(AbstractArtefact value) {
//        List<Field> allFieldsInArtefactHierarchy = new ArrayList<>();
//        Class<?> currentClass = value.getClass();
//        while (currentClass != null) {
//            allFieldsInArtefactHierarchy.addAll(List.of(currentClass.getDeclaredFields()));
//            currentClass = currentClass.getSuperclass();
//        }
//        Collections.reverse(allFieldsInArtefactHierarchy);
//        for (Field field : allFieldsInArtefactHierarchy) {
//            field.setAccessible(true);
//        }
//        return allFieldsInArtefactHierarchy;
//    }

}
