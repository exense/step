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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistryAware;
import step.automation.packages.yaml.model.AbstractAutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYamlImpl;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;
import step.core.yaml.SerializationUtils;

import java.io.IOException;
import java.util.*;

@StepYamlDeserializerAddOn(targetClasses = {AutomationPackageFragmentYamlImpl.class})
public class YamlAutomationPackageFragmentDeserializer<T extends AutomationPackageDescriptorYamlImpl> extends StepYamlDeserializer<AbstractAutomationPackageFragmentYaml>
    implements AutomationPackageSerializationRegistryAware {

    protected AutomationPackageSerializationRegistry registry;

    public YamlAutomationPackageFragmentDeserializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public AbstractAutomationPackageFragmentYaml deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonDeserializer<Object> defaultDeserializerForClass = getDefaultDeserializerForClass(p, ctxt, getObjectClass());
        ObjectCodec oc = p.getCodec();
        JsonNode node = oc.readTree(p);

        ObjectNode nonBasicFields = node.deepCopy();
        Class<?> clazz = getObjectClass();
        List<String> basicFields = SerializationUtils.getJsonFieldNames(yamlObjectMapper, clazz);
        nonBasicFields.remove(basicFields);

        try (JsonParser treeParser = oc.treeAsTokens(node)) {
            ctxt.getConfig().initialize(treeParser);

            if (treeParser.getCurrentToken() == null) {
                treeParser.nextToken();
            }
            AbstractAutomationPackageFragmentYaml res = (AbstractAutomationPackageFragmentYaml) defaultDeserializerForClass.deserialize(treeParser, ctxt);

            if (registry != null) {
                Map<String, List<?>> nonBasicFieldsMap = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = nonBasicFields.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> next = fields.next();
                    List<Object> list = new ArrayList<>();
                    if (next.getValue() != null) {
                        // acquire reader for the right type
                        Class<?> targetClass = registry.resolveClassForYamlField(next.getKey());
                        if (targetClass != null) {
                            list = yamlObjectMapper.readerForListOf(targetClass).readValue(next.getValue());
                        }
                    }
                    nonBasicFieldsMap.put(next.getKey(), list);
                }
                res.setAdditionalFields(nonBasicFieldsMap);
            }
            return res;
        }

    }

    protected Class<?> getObjectClass() {
        return AutomationPackageFragmentYamlImpl.class;
    }

    @Override
    public void setSerializationRegistry(AutomationPackageSerializationRegistry registry) {
        this.registry = registry;
    }
}
