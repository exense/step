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
package step.plans.parser.yaml.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.artefacts.AbstractArtefact;
import step.core.yaml.deserializers.NamedEntityYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;
import step.plans.parser.yaml.YamlArtefactsLookuper;
import step.plans.parser.yaml.SerializationUtils;
import step.plans.parser.yaml.model.AbstractYamlArtefact;
import step.plans.parser.yaml.model.NamedYamlArtefact;
import step.plans.parser.yaml.model.SimpleYamlArtefact;

import java.io.IOException;
import java.util.List;

@StepYamlDeserializerAddOn(targetClasses = {NamedYamlArtefact.class})
public class NamedYamlArtefactDeserializer extends StepYamlDeserializer<NamedYamlArtefact> {

    public NamedYamlArtefactDeserializer() {
        this(null);
    }

    public NamedYamlArtefactDeserializer(ObjectMapper stepYamlObjectMapper) {
        super(stepYamlObjectMapper);
    }

    @Override
    public NamedYamlArtefact deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        NamedEntityYamlDeserializer<AbstractYamlArtefact<?>> namedEntityDeserializer = new NamedEntityYamlDeserializer<>() {
            @Override
            protected String resolveTargetClassNameByYamlName(String yamlName) {
                return null;
            }

            protected Class<?> resolveTargetClassByYamlName(String yamlName) {
                return YamlArtefactsLookuper.getArtefactModelClassByYamlName(yamlName);
            }
        };

        String entityName = namedEntityDeserializer.getEntityNameFromYaml(node);
        Class<?> artefactClass = YamlArtefactsLookuper.getArtefactModelClassByYamlName(entityName);
        if (artefactClass != null && AbstractYamlArtefact.class.isAssignableFrom(artefactClass)) {
            return new NamedYamlArtefact(namedEntityDeserializer.deserialize(node, jsonParser.getCodec()));
        } else if (artefactClass != null && AbstractArtefact.class.isAssignableFrom(artefactClass)) {
            JsonNode artefactFields = node.get(entityName);
            ObjectNode nonBasicFields = artefactFields.deepCopy();
            List<String> basicFields = SerializationUtils.getJsonFieldNames(yamlObjectMapper, AbstractYamlArtefact.class);
            for (String basicField : basicFields) {
                nonBasicFields.remove(basicField);
            }
            SimpleYamlArtefact<AbstractArtefact> simpleYamlArtefact = new SimpleYamlArtefact<>((Class<AbstractArtefact>) artefactClass, nonBasicFields);
            yamlObjectMapper.readerForUpdating(simpleYamlArtefact).readValue(artefactFields);
            return new NamedYamlArtefact(simpleYamlArtefact);
        } else {
            throw new RuntimeException("Unable to resolve java class for artefact " + entityName);
        }
    }


}
