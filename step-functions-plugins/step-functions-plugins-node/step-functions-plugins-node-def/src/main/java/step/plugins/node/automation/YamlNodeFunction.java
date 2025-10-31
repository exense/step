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
package step.plugins.node.automation;

import step.automation.packages.AutomationPackageResourceUploader;
import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.model.AbstractYamlFunction;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.YamlModel;
import step.plugins.node.NodeFunction;
import step.resources.ResourceManager;

@YamlModel(name = "Node")
public class YamlNodeFunction extends AbstractYamlFunction<NodeFunction> {

    @YamlFieldCustomCopy
    private DynamicValue<String> jsfile = new DynamicValue<>();

    public DynamicValue<String> getJsfile() {
        return jsfile;
    }

    public void setJsfile(DynamicValue<String> jsfile) {
        this.jsfile = jsfile;
    }

    @Override
    protected void fillDeclaredFields(NodeFunction function, StagingAutomationPackageContext context) {
        super.fillDeclaredFields(function, context);
        AutomationPackageResourceUploader resourceUploader = new AutomationPackageResourceUploader();

        String filePath = jsfile.get();
        String fileRef = resourceUploader.applyResourceReference(filePath, ResourceManager.RESOURCE_TYPE_FUNCTIONS, context);
        if (fileRef != null) {
            function.setJsFile(new DynamicValue<>(fileRef));
        }
    }

    @Override
    protected NodeFunction createFunctionInstance() {
        return new NodeFunction();
    }
}
