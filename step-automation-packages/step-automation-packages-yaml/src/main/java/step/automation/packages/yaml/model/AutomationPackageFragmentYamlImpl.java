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
package step.automation.packages.yaml.model;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.core.yaml.deserialization.PatchingContext;

public class AutomationPackageFragmentYamlImpl extends AbstractAutomationPackageFragmentYaml {

    @JsonCreator
    public AutomationPackageFragmentYamlImpl(
        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper mapper,
        @JacksonInject(useInput = OptBoolean.FALSE) AutomationPackageSerializationRegistry serializationRegistry,
        @JacksonInject(useInput = OptBoolean.FALSE) PatchingContext patchingContext
        ) {
        super(mapper, serializationRegistry, patchingContext);
    }

    public AutomationPackageFragmentYamlImpl(PatchingContext context) {
        this(null, null, context);
    }
}
