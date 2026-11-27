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
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.yaml.PatchingContext;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;

import java.io.IOException;

@StepYamlDeserializerAddOn(targetClasses = {AutomationPackageFragmentYamlImpl.class})
public class YamlAutomationPackageFragmentDeserializer extends AbstractYamlAutomationPackageFragmentDeserializer {

    public YamlAutomationPackageFragmentDeserializer(BeanDeserializer deserializer) {
        super(deserializer);
    }

    @Override
    protected YamlAutomationPackageFragmentDeserializer withDelegate(BeanDeserializer contextualDelegate) {
        return new YamlAutomationPackageFragmentDeserializer(contextualDelegate);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return deserialize(p, ctxt, new AutomationPackageFragmentYamlImpl((PatchingContext) ctxt.getAttribute(PatchingContext.class)));
    }
}
