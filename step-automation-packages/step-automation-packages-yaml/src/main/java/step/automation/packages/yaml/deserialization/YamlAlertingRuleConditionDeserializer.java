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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.core.yaml.deserializers.NamedEntityYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;
import step.core.yaml.deserializers.YamlFieldDeserializationProcessor;
import step.plugins.alerting.rule.condition.AlertingRuleCondition;

import java.io.IOException;
import java.util.List;

@StepYamlDeserializerAddOn(targetClasses = {AlertingRuleCondition.class})
public class YamlAlertingRuleConditionDeserializer extends StepYamlDeserializer<AlertingRuleCondition> {

    public YamlAlertingRuleConditionDeserializer() {
    }

    public YamlAlertingRuleConditionDeserializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public AlertingRuleCondition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return new NamedEntityYamlDeserializer<AlertingRuleCondition>() {
            @Override
            protected Class<?> resolveTargetClassByYamlName(String yamlName) {
                List<Class<?>> classes = AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AlertingRuleCondition.class);
                return AutomationPackageNamedEntityUtils.getClassByEntityName(yamlName, classes);
            }

            @Override
            protected List<YamlFieldDeserializationProcessor> deserializationProcessors() {
                return AutomationPackageNamedEntityUtils.scanDeserializationProcessorsForNamedEntity(AlertingRuleCondition.class);
            }

            @Override
            protected String getTargetClassField() {
                return AlertingRuleCondition.JSON_CLASS_FIELD;
            }
        }.deserialize(p.getCodec().readTree(p), p.getCodec());
    }
}
