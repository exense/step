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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.yaml.YamlModelUtils;
import step.core.yaml.serializers.NamedEntityYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;
import step.core.yaml.model.AbstractYamlArtefact;
import step.core.yaml.model.NamedYamlArtefact;
import step.core.yaml.model.SimpleYamlArtefact;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@StepYamlSerializerAddOn(targetClasses = {NamedYamlArtefact.class})
public class NamedYamlArtefactSerializer extends StepYamlSerializer<NamedYamlArtefact> {

    private static final Logger log = LoggerFactory.getLogger(NamedYamlArtefactSerializer.class);

    public NamedYamlArtefactSerializer() {
        super();
    }

    public NamedYamlArtefactSerializer(ObjectMapper stepYamlMapper) {
        super(stepYamlMapper);
    }

    @Override
    public void serialize(NamedYamlArtefact value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        NamedEntityYamlSerializer<AbstractYamlArtefact<?>> ser = new NamedEntityYamlSerializer<>() {
            @Override
            protected String resolveYamlName(AbstractYamlArtefact<?> value) {
                return YamlModelUtils.getEntityNameByClass(value.getArtefactClass());
            }

            @Override
            protected void writeObject(AbstractYamlArtefact<?> value, JsonGenerator gen) throws IOException {
                if (value instanceof SimpleYamlArtefact<?>) {
                    Class<?> artefactClass = value.getArtefactClass();
                    SimpleYamlArtefact defaultYamlArtefact = new SimpleYamlArtefact(artefactClass, null, yamlObjectMapper);
                    ObjectNode defaultJson = defaultYamlArtefact.toFullJson();
                    // TODO: remove default values from result
                    ObjectNode actualJson = ((SimpleYamlArtefact<?>) value).toFullJson();
                    gen.writeTree(actualJson);
                } else {
                    try {
                        AbstractYamlArtefact<?> defaultInstance = value.getClass().getConstructor().newInstance();
                        ObjectNode defaultJsonNode = (ObjectNode) yamlObjectMapper.valueToTree(defaultInstance);
                        ObjectNode actualValue = yamlObjectMapper.valueToTree(value);
                        removeDefaultValues(actualValue, defaultJsonNode);
                        gen.writeObject(actualValue);
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to serialize artifact: " + value.getClass(), e);
                    }
                }
            }
        };
        ser.serialize(value.getYamlArtefact(), gen, serializers);
    }

    protected void removeDefaultValues(ObjectNode actualJson, ObjectNode defaultJson){
        List<String> fieldsForRemoval = new ArrayList<>();
        actualJson.fieldNames().forEachRemaining(s -> {
            JsonNode defaultValue = defaultJson.get(s);
            if(Objects.equals(defaultValue, actualJson.get(s))){
                fieldsForRemoval.add(s);
            }
        });
        for (String s : fieldsForRemoval) {
            actualJson.remove(s);
        }
        // TODO: for children also
    }

}
