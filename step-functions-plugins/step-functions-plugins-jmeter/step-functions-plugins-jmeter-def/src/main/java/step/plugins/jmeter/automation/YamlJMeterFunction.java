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
package step.plugins.jmeter.automation;

import com.fasterxml.jackson.annotation.JsonInclude;
import step.automation.packages.AutomationPackageLocalResourceMapper;
import step.automation.packages.AutomationPackageResourceMapper;
import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.model.AbstractYamlFunction;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.YamlModel;
import step.plugins.jmeter.JMeterFunction;

@YamlModel(name = "JMeter")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class YamlJMeterFunction extends AbstractYamlFunction<JMeterFunction> {

    // An empty value rather than none to support serialization
    @YamlFieldCustomCopy
    private DynamicValue<String> jmeterTestplan = new DynamicValue<>("");

    public DynamicValue<String> getJmeterTestplan() {
        return jmeterTestplan;
    }

    public void setJmeterTestplan(DynamicValue<String> jmeterTestplan) {
        this.jmeterTestplan = jmeterTestplan;
    }

    @Override
    protected void fillDeclaredFields(JMeterFunction function, StagingAutomationPackageContext context) {
        super.fillDeclaredFields(function, context);
        AutomationPackageResourceMapper resourceMapper = context.getResourceMapper();

        String testplanPath = jmeterTestplan.get();
        String testPlanRef = resourceMapper.applyResourceReference(testplanPath, context);
        if (testPlanRef != null) {
            function.setJmeterTestplan(new DynamicValue<>(testPlanRef));
        }
    }

    @Override
    public void setDeclaredFieldsFromObject(JMeterFunction function) {
        super.setDeclaredFieldsFromObject(function);
        jmeterTestplan = AutomationPackageLocalResourceMapper.toDescriptorReference(function.getJmeterTestplan());
    }

    @Override
    protected JMeterFunction createFunctionInstance() {
        return new JMeterFunction();
    }

}
