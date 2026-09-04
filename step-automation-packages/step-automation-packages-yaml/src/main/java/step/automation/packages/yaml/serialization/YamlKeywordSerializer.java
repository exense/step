package step.automation.packages.yaml.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.core.yaml.YamlModelUtils;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;

import java.io.IOException;

/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
@StepYamlSerializerAddOn(targetClasses = {YamlAutomationPackageKeyword.class})
public class YamlKeywordSerializer extends StepYamlSerializer<YamlAutomationPackageKeyword> {


    public YamlKeywordSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public void serialize(YamlAutomationPackageKeyword yamlKeyword, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName(YamlModelUtils.getEntityNameByClass(yamlKeyword.getYamlKeyword().getClass()));
        gen.writeObject(yamlKeyword.getYamlKeyword());
        gen.writeEndObject();
    }
}
