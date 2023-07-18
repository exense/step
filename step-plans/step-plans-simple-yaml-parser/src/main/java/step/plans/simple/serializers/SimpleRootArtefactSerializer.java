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
package step.plans.simple.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.scanner.CachedAnnotationScanner;
import step.plans.simple.ArtefactFieldMetadataExtractor;
import step.plans.simple.YamlPlanSerializerExtender;
import step.plans.simple.YamlPlanSerializerExtension;
import step.plans.simple.model.SimpleRootArtefact;
import step.plans.simple.rules.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static step.core.scanner.Classes.newInstanceAs;

public class SimpleRootArtefactSerializer extends JsonSerializer<SimpleRootArtefact> {

    private final List<SimpleArtefactFieldSerializationProcessor> customFieldProcessors;
    private final ArtefactFieldMetadataExtractor metadataExtractor;

    public SimpleRootArtefactSerializer() {
        this.metadataExtractor = prepareMetadataExtractor();
        this.customFieldProcessors = prepareFieldProcessors();
    }

    protected ArtefactFieldMetadataExtractor prepareMetadataExtractor() {
        return new ArtefactFieldMetadataExtractor();
    }

    protected List<SimpleArtefactFieldSerializationProcessor> prepareFieldProcessors() {
        List<SimpleArtefactFieldSerializationProcessor> result =  new ArrayList<>();

        // -- BASIC PROCESSING RULES

        result.add(new TechnicalFieldRule().getArtefactFieldSerializationProcessor());
        result.add(new CommonFilteredFieldRule().getArtefactFieldSerializationProcessor());
        result.add(new NodeNameRule().getArtefactFieldSerializationProcessor());
        result.add((artefact, field, fieldMetadata, gen) -> {
            if (field.getName().equals("children")) {
                List<AbstractArtefact> children = (List<AbstractArtefact>) field.get(artefact);
                if(children != null && !children.isEmpty()) {
                    gen.writeFieldName("children");
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
        result.add(new KeywordSelectionRule().getArtefactFieldSerializationProcessor());
        result.add(new KeywordInputsRule().getArtefactFieldSerializationProcessor());
        result.add(new FunctionGroupSelectionRule().getArtefactFieldSerializationProcessor());
        result.add(new CheckExpressionRule().getArtefactFieldSerializationProcessor());

        return result;
    }

    protected List<SimpleArtefactFieldSerializationProcessor> getExtensions() {
        List<SimpleArtefactFieldSerializationProcessor> extensions = new ArrayList<>();
        CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanSerializerExtension.class).stream()
                .map(newInstanceAs(YamlPlanSerializerExtender.class)).forEach(e -> extensions.addAll(e.getSerializationExtensions()));
        return extensions;
    }

    @Override
    public void serialize(SimpleRootArtefact value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        AbstractArtefact artefact = value.getAbstractArtefact();

        // root artefact
        try {
            processArtefact(gen, artefact);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to serialize artefact " + value.getClass(), e);
        }
    }

    private void processArtefact(JsonGenerator gen, AbstractArtefact artefact) throws IOException, IllegalAccessException {
        gen.writeStartObject();
        String artefactName = AbstractArtefact.getArtefactName(artefact.getClass());
        gen.writeFieldName(artefactName);
        gen.writeStartObject();
        serializeArtefactFields(artefact, gen);
        gen.writeEndObject();
        gen.writeEndObject();
    }


    protected void serializeArtefactFields(AbstractArtefact value, JsonGenerator gen) throws IOException, IllegalAccessException {
        List<Field> allFieldsInArtefact = getAllFieldsInArtefact(value);
        for (Field field : allFieldsInArtefact) {
            boolean processedAsCustomField = false;
            for (SimpleArtefactFieldSerializationProcessor customFieldProcessor : customFieldProcessors) {
                if(customFieldProcessor.serializeArtefactField(value, field, metadataExtractor.extractMetadata(field), gen)){
                    processedAsCustomField = true;
                    break;
                }
            }
            try {
                if (!processedAsCustomField) {
                    // default processing
                    Object fieldValue = field.get(value);
                    if(fieldValue != null) {
                        gen.writeObjectField(field.getName(), fieldValue);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to get field value (" + field.getName() + ")", e);
            }
        }
    }

    private List<Field> getAllFieldsInArtefact(AbstractArtefact value) {
        List<Field> allFieldsInArtefactHierarchy = new ArrayList<>();
        Class<?> currentClass = value.getClass();
        while (currentClass != null) {
            allFieldsInArtefactHierarchy.addAll(List.of(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        Collections.reverse(allFieldsInArtefactHierarchy);
        for (Field field : allFieldsInArtefactHierarchy) {
            field.setAccessible(true);
        }
        return allFieldsInArtefactHierarchy;
    }

}
