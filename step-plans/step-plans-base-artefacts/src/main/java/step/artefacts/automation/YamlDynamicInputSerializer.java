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
package step.artefacts.automation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;
import step.plans.parser.yaml.DynamicInputsSupport;

import java.io.IOException;

@StepYamlSerializerAddOn(targetClasses = {YamlDynamicInputs.class})
public class YamlDynamicInputSerializer extends StepYamlSerializer<YamlDynamicInputs> {

    private final DynamicInputsSupport dynamicInputsSupport;

    public YamlDynamicInputSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
        this.dynamicInputsSupport = new DynamicInputsSupport();
    }

    @Override
    public void serialize(YamlDynamicInputs value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        dynamicInputsSupport.serializeDynamicInputs(gen, value.toDynamicValue());
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, YamlDynamicInputs value) {
        return !dynamicInputsSupport.isNotEmptyDynamicInputs(value.toDynamicValue());
    }
}
