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
import step.core.artefacts.AbstractArtefact;
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
import java.util.Set;
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
                try {
                    // Here in both branches we create the default instance of AbstractYamlArtefact and serialize it (to default json)
                    // to find out all fields with values matching to default ones and remove these fields from resulting
                    // json (for example, to avoid generation of redundant continueParentNodeExecutionOnError, instrumentNode
                    // and skipNode fields for each artefact)
                    if (value instanceof SimpleYamlArtefact<?>) {
                        AbstractArtefact defaultTechnicalInstance = value.createArtefactInstance();
                        SimpleYamlArtefact<?> defaultYamlArtefact = (SimpleYamlArtefact<?>) AbstractYamlArtefact.toYamlArtefact(defaultTechnicalInstance, yamlObjectMapper);
                        ObjectNode defaultJson = defaultYamlArtefact.toFullJson();
                        ObjectNode actualJson = ((SimpleYamlArtefact<?>) value).toFullJson();
                        removeDefaultValues(actualJson, defaultJson);
                        gen.writeTree(actualJson);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Analyze default values for " + value.getClass().getName());
                        }
                        AbstractArtefact defaultTechnicalInstance = value.createArtefactInstance();
                        AbstractYamlArtefact<?> defaultYamlInstance = AbstractYamlArtefact.toYamlArtefact(defaultTechnicalInstance, yamlObjectMapper);
                        ObjectNode defaultJson = yamlObjectMapper.valueToTree(defaultYamlInstance);
                        ObjectNode actualValue = yamlObjectMapper.valueToTree(value);
                        removeDefaultValues(actualValue, defaultJson);
                        gen.writeObject(actualValue);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Unable to serialize artifact: " + value.getClass(), e);
                }
            }
        };
        ser.serialize(value.getYamlArtefact(), gen, serializers);
    }

}
