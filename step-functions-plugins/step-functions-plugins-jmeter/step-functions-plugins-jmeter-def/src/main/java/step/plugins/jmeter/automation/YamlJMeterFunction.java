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

import step.automation.packages.AutomationPackageContext;
import step.automation.packages.AutomationPackageResourceUploader;
import step.automation.packages.model.AbstractYamlFunction;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlModel;
import step.plugins.jmeter.JMeterFunction;
import step.resources.ResourceManager;

@YamlModel(name = "JMeter")
public class YamlJMeterFunction extends AbstractYamlFunction<JMeterFunction> {

    private DynamicValue<String> jmeterTestplan = new DynamicValue<>();

    public DynamicValue<String> getJmeterTestplan() {
        return jmeterTestplan;
    }

    public void setJmeterTestplan(DynamicValue<String> jmeterTestplan) {
        this.jmeterTestplan = jmeterTestplan;
    }

    @Override
    protected void fillDeclaredFields(JMeterFunction function, AutomationPackageContext context) {
        super.fillDeclaredFields(function, context);
        AutomationPackageResourceUploader resourceUploader = new AutomationPackageResourceUploader();

        String testplanPath = jmeterTestplan.get();
        String testPlanRef = resourceUploader.applyResourceReference(testplanPath, ResourceManager.RESOURCE_TYPE_FUNCTIONS, context);
        if (testPlanRef != null) {
            function.setJmeterTestplan(new DynamicValue<>(testPlanRef));
        }
    }

    @Override
    protected JMeterFunction createFunctionInstance() {
        return new JMeterFunction();
    }

}
