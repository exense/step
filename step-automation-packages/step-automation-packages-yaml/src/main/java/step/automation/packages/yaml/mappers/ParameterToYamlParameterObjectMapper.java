package step.automation.packages.yaml.mappers;

import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.yaml.deserialization.PatchableYamlList;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParameter;

import java.util.Optional;

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

@ObjectToYamlMapping(organizableObject = Parameter.class)
public class ParameterToYamlParameterObjectMapper extends ObjectToYamlObjectMapper<Parameter, AutomationPackageParameter> {

    @Override
    public AutomationPackageParameter getNewYamlObject(Parameter parameter) {
        return AutomationPackageParameter.fromParameter(parameter);
    }

    @Override
    public Optional<Parameter> getBusinessObject(AutomationPackageParameter yamlParameter) {
        return Optional.ofNullable(yamlParameter.toParameter());
    }

    @Override
    public PatchableYamlList<AutomationPackageParameter> getListInFragment(AutomationPackageFragmentYaml fragment) {
        return (PatchableYamlList<AutomationPackageParameter>) fragment.getAdditionalFields()
            .computeIfAbsent(Parameter.ENTITY_NAME, f -> new PatchableYamlList<AutomationPackageParameter>(fragment.getPatchingContext(), f));
    }

    @Override
    public String getCollectionName() {
        return Parameter.ENTITY_NAME;
    }
}
