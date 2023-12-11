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
package step.engine.plugins;

import ch.exense.commons.app.Configuration;
import step.automation.packages.AutomationPackageArchive;
import step.automation.packages.AutomationPackageKeywordsAttributesApplier;
import step.automation.packages.AutomationPackageReader;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.resources.ResourceManager;

import java.util.ArrayList;
import java.util.List;

@Plugin(dependencies = {FunctionPlugin.class, AutomationPackageJsonSchemaPlugin.class})
public class AutomationPackageKeywordsPlugin extends AbstractExecutionEnginePlugin {

    private FunctionAccessor functionAccessor;
    private ResourceManager resourceManager;

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (context.getOperationMode() == OperationMode.LOCAL) {
            functionAccessor = context.require(FunctionAccessor.class);
            resourceManager = context.getResourceManager();

            List<Function> localFunctions = getFunctionsFromAutomationPackage(context.getConfiguration());
            functionAccessor.save(localFunctions);
        }
    }

    public List<Function> getFunctionsFromAutomationPackage(Configuration configuration) {
        ArrayList<Function> res = new ArrayList<>();

        // the automation package should be found in current classloader
        AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(this.getClass().getClassLoader());
        if (automationPackageArchive.isAutomationPackage()) {
            try {
                AutomationPackageReader automationPackageReader = new AutomationPackageReader(getAutomationPackageJsonSchema(configuration));
                AutomationPackageKeywordsAttributesApplier attributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);
                AutomationPackageContent automationPackageContent = automationPackageReader.readAutomationPackage(automationPackageArchive, true);
                if (automationPackageContent != null) {
                    for (AutomationPackageKeyword foundKeyword : automationPackageContent.getKeywords()) {
                        res.add(attributesApplier.applySpecialAttributesToKeyword(foundKeyword, automationPackageArchive, null, null));
                    }
                }
            } catch (AutomationPackageReadingException e) {
                throw new RuntimeException("Unable to extract functions from automation package", e);
            }
        }
        return res;
    }

    protected String getAutomationPackageJsonSchema(Configuration configuration) {
        return configuration.getProperty(AutomationPackageJsonSchemaPlugin.PROP_AUTOMATION_PACKAGE_JSON_SCHEMA, null);
    }
}
