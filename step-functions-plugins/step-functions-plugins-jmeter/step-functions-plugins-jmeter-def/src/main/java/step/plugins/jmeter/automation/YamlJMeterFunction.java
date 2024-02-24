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

import step.attachments.FileResolver;
import step.automation.packages.AutomationPackageAttributesApplyingContext;
import step.automation.packages.AutomationPackageNamedEntity;
import step.automation.packages.AutomationPackageResourceUploader;
import step.automation.packages.model.AbstractYamlKeyword;
import step.core.dynamicbeans.DynamicValue;
import step.plugins.jmeter.JMeterFunction;
import step.resources.Resource;
import step.resources.ResourceManager;

@AutomationPackageNamedEntity(name = "JMeter")
public class YamlJMeterFunction extends AbstractYamlKeyword<JMeterFunction> {

    private DynamicValue<String> jmeterTestplan = new DynamicValue<>();

    public DynamicValue<String> getJmeterTestplan() {
        return jmeterTestplan;
    }

    public void setJmeterTestplan(DynamicValue<String> jmeterTestplan) {
        this.jmeterTestplan = jmeterTestplan;
    }

    @Override
    protected void fillAdditionalFields(JMeterFunction keyword) {

    }

    @Override
    public JMeterFunction toFullKeyword(AutomationPackageAttributesApplyingContext context) {
        JMeterFunction res = super.toFullKeyword(context);

        AutomationPackageResourceUploader resourceUploader = new AutomationPackageResourceUploader();

        String testplanPath = jmeterTestplan.get();
        String testPlanRef = null;

        if (testplanPath != null && !testplanPath.startsWith(FileResolver.RESOURCE_PREFIX)) {
            Resource resource = resourceUploader.uploadResourceFromAutomationPackage(testplanPath, ResourceManager.RESOURCE_TYPE_FUNCTIONS, context);
            if (resource != null) {
                testPlanRef = FileResolver.RESOURCE_PREFIX + resource.getId().toString();
            }
        } else {
            testPlanRef = testplanPath;
        }

        if (testPlanRef != null) {
            res.setJmeterTestplan(new DynamicValue<>(testPlanRef));
        }

        return res;
    }

    @Override
    protected JMeterFunction createKeywordInstance() {
        return new JMeterFunction();
    }
}
