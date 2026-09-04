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
package step.automation.packages.yaml.mappers;

import step.automation.packages.mappers.AbstractFunctionToYamlMapper;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapping;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.automation.YamlGeneralScriptFunction;

@BusinessObjectToYamlMapping(sourceClass = GeneralScriptFunction.class)
public class GeneralScriptFunctionToYamlMapper extends AbstractFunctionToYamlMapper<GeneralScriptFunction> {

    @Override
    public YamlAutomationPackageKeyword toYamlObject(GeneralScriptFunction generalScriptFunction) {

        YamlGeneralScriptFunction yamlFunction = new YamlGeneralScriptFunction();
        // the custom-copy fields, resource references included, are mapped back by the yaml model itself
        setCommonAttributes(generalScriptFunction, yamlFunction);

        return new YamlAutomationPackageKeyword(yamlFunction, null);
    }

}
