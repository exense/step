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
package step.plugins.java.automation;

import step.automation.packages.AutomationPackageContext;
import step.automation.packages.AutomationPackageResourceUploader;
import step.automation.packages.model.AbstractYamlFunction;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.YamlModel;
import step.plugins.java.GeneralFunctionScriptLanguage;
import step.plugins.java.GeneralScriptFunction;
import step.resources.ResourceManager;

@YamlModel(name = "GeneralScript")
public class YamlGeneralScriptFunction extends AbstractYamlFunction<GeneralScriptFunction> {

    @YamlFieldCustomCopy
    private DynamicValue<String> scriptFile = new DynamicValue<>("");

    @YamlFieldCustomCopy
    private DynamicValue<String> librariesFile = new DynamicValue<>("");

    @YamlFieldCustomCopy
    private GeneralFunctionScriptLanguage scriptLanguage = null;

    @Override
    protected void fillDeclaredFields(GeneralScriptFunction res, AutomationPackageContext context) {
        super.fillDeclaredFields(res, context);
        if (scriptLanguage != null) {
            res.setScriptLanguage(new DynamicValue<>(scriptLanguage.name()));
        } else {
            // groovy is default value
            res.setScriptLanguage(new DynamicValue<>(GeneralFunctionScriptLanguage.groovy.name()));
        }

        AutomationPackageResourceUploader resourceUploader = new AutomationPackageResourceUploader();
        String scriptFilePath = scriptFile.get();
        String uploaded = resourceUploader.applyResourceReference(scriptFilePath, ResourceManager.RESOURCE_TYPE_FUNCTIONS, context);
        if (uploaded != null) {
            res.setScriptFile(new DynamicValue<>(uploaded));
        }

        // TODO: should be taken from library file uploaded with automation package
        String librariesFilePath = librariesFile.get();
        uploaded = resourceUploader.applyResourceReference(librariesFilePath, ResourceManager.RESOURCE_TYPE_FUNCTIONS, context);
        if (uploaded != null) {
            res.setLibrariesFile(new DynamicValue<>(uploaded));
        }
    }

    @Override
    protected GeneralScriptFunction createFunctionInstance() {
        return new GeneralScriptFunction();
    }

    public DynamicValue<String> getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(DynamicValue<String> scriptFile) {
        this.scriptFile = scriptFile;
    }

    public DynamicValue<String> getLibrariesFile() {
        return librariesFile;
    }

    public void setLibrariesFile(DynamicValue<String> librariesFile) {
        this.librariesFile = librariesFile;
    }

    public GeneralFunctionScriptLanguage getScriptLanguage() {
        return scriptLanguage;
    }

    public void setScriptLanguage(GeneralFunctionScriptLanguage scriptLanguage) {
        this.scriptLanguage = scriptLanguage;
    }
}
