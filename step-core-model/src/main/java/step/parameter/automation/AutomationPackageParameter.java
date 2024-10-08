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
package step.parameter.automation;

import step.commons.activation.Expression;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.AbstractYamlModel;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.YamlModel;
import step.parameter.Parameter;
import step.parameter.ParameterScope;

@YamlModel(named = false)
public class AutomationPackageParameter extends AbstractYamlModel {

    protected String key;
    protected DynamicValue<String> value;
    protected String description;

    @YamlFieldCustomCopy
    protected String activationScript;

    protected Integer priority;
    protected Boolean protectedValue = false;
    protected ParameterScope scope = ParameterScope.GLOBAL;
    protected String scopeEntity;

    public Parameter toParameter() {
        Parameter res = new Parameter();
        copyFieldsToObject(res, true);
        if (activationScript != null) {
            res.setActivationExpression(new Expression(activationScript));
        }
        return res;
    }

    public String getKey() {
        return key;
    }

    public DynamicValue<String> getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getActivationScript() {
        return activationScript;
    }

    public Integer getPriority() {
        return priority;
    }

    public Boolean getProtectedValue() {
        return protectedValue;
    }

    public ParameterScope getScope() {
        return scope;
    }

    public String getScopeEntity() {
        return scopeEntity;
    }
}
