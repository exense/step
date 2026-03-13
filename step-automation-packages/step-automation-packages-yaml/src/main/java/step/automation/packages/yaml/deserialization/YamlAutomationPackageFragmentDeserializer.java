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
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.util.LinkedNode;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistryAware;
import step.automation.packages.yaml.model.AbstractAutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYamlImpl;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;

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
        AbstractAutomationPackageFragmentYaml res = (AbstractAutomationPackageFragmentYaml) defaultDeserializerForClass.deserialize(p, ctxt);
        
        LinkedNode<DeserializationProblemHandler> handlers = ctxt.getConfig().getProblemHandlers();
        if (handlers != null) {
            AdditionalFieldHandler handler = (AdditionalFieldHandler) ctxt.getConfig().getProblemHandlers().value();
            if (handler != null) {
                res.setAdditionalFields(handler.getAdditionalFields());
            }
        }
        
        return res;
    }

    protected Class<?> getObjectClass() {
        return AutomationPackageFragmentYamlImpl.class;
    }

    @Override
    public void setSerializationRegistry(AutomationPackageSerializationRegistry registry) {
        this.registry = registry;
    }
}
