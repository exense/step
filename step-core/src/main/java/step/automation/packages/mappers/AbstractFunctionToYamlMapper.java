package step.automation.packages.mappers;

import step.automation.packages.model.AbstractYamlFunction;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapper;
import step.core.accessors.AbstractOrganizableObject;
import step.functions.Function;

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
public abstract class AbstractFunctionToYamlMapper<F extends Function> implements BusinessObjectToYamlMapper<F, YamlAutomationPackageKeyword> {


    protected void setCommonAttributes(F function, AbstractYamlFunction<F> yamlFunction) {
        yamlFunction.setDeclaredFieldsFromObject(function);
    }

    @Override
    public String getCollectionName() {
        return YamlAutomationPackageKeyword.KEYWORDS_ENTITY_NAME;
    }
}
