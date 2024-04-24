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
package step.artefacts.automation.datasource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.yaml.deserializers.NamedEntityYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;

import java.io.IOException;

@StepYamlDeserializerAddOn(targetClasses = {NamedYamlDataSource.class})
public class NamedYamlDataSourceDeserializer extends StepYamlDeserializer<NamedYamlDataSource> {

    public NamedYamlDataSourceDeserializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public NamedYamlDataSource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        NamedEntityYamlDeserializer<AbstractYamlDataSource<?>> namedEntityDeserializer = new NamedEntityYamlDeserializer<>() {
            @Override
            protected String resolveTargetClassNameByYamlName(String yamlName) {
                return null;
            }

            protected Class<?> resolveTargetClassByYamlName(String yamlName) {
                return YamlDataSourceLookuper.getModelClassByYamlName(yamlName);
            }
        };

        return new NamedYamlDataSource(namedEntityDeserializer.deserialize(node, p.getCodec()));
    }
}
