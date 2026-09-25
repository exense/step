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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import step.core.yaml.PatchingContext;
import step.core.yaml.deserialization.PatchableYamlScalarField;

import java.util.HashMap;
import java.util.Map;

public class AutomationPackageDescriptorYamlImpl extends AbstractAutomationPackageFragmentYaml implements AutomationPackageDescriptorYaml {

    private PatchableYamlScalarField<String> version;

    private PatchableYamlScalarField<String> name;

    private Map<String, String> attributes = new HashMap<>();


    public AutomationPackageDescriptorYamlImpl(@JacksonInject(useInput = OptBoolean.FALSE) PatchingContext patchingContext) {
        super(patchingContext);
        version = new PatchableYamlScalarField<>(patchingContext, VERSION_FIELD_NAME, null);
        name = new PatchableYamlScalarField<>(patchingContext, NAME_FIELD_NAME, null);
    }

    @Override
    public PatchableYamlScalarField<String> getName() {
        return name;
    }

    @Override
    public void setName(PatchableYamlScalarField<String> name) {
        this.name = name;
    }

    @Override
    public PatchableYamlScalarField<String> getVersion() {
        return version;
    }

    public void setVersion(PatchableYamlScalarField<String> version) {
        this.version = version;
    }

    @Override
    @JsonIgnore
    public void setVersionString(String versionString) {
        version.setValue(versionString);
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
