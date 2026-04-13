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
package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.automation.packages.model.AbstractYamlFunction;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.core.yaml.AutomationPackageKeywordsLookuper;
import step.core.yaml.deserializers.NamedEntityYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;

import java.io.IOException;

@StepYamlDeserializerAddOn(targetClasses = {YamlAutomationPackageKeyword.class})
public class YamlKeywordDeserializer extends StepYamlDeserializer<YamlAutomationPackageKeyword> {

    private final AutomationPackageKeywordsLookuper keywordsLookuper;

    public YamlKeywordDeserializer() {
        this(null);
    }

    public YamlKeywordDeserializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
        this.keywordsLookuper = new AutomationPackageKeywordsLookuper();
    }

    @Override
    public YamlAutomationPackageKeyword deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String yamlName = jsonParser.nextFieldName();

        try {
            Class<?> clazz = Class.forName(keywordsLookuper.yamlKeywordClassToJava(yamlName));
            jsonParser.nextToken();
            YamlAutomationPackageKeyword keyword = new YamlAutomationPackageKeyword((AbstractYamlFunction<?>) deserializationContext.readValue(jsonParser, clazz));
            jsonParser.nextToken();
            return keyword;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
