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

import org.bson.types.ObjectId;
import step.automation.packages.*;
import step.automation.packages.model.AutomationPackageContent;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.resources.ResourceManager;

import java.util.ArrayList;
import java.util.List;

@Plugin(dependencies = {FunctionPlugin.class})
public class AutomationPackageKeywordsPlugin extends AbstractExecutionEnginePlugin {

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (context.getOperationMode() == OperationMode.LOCAL) {
            FunctionAccessor functionAccessor = context.require(FunctionAccessor.class);
            ResourceManager resourceManager = context.getResourceManager();

            List<Function> localFunctions = getFunctionsFromAutomationPackage(
                    resourceManager,
                    context.computeIfAbsent(
                            AutomationPackageManager.class,
                            automationPackageManagerClass -> AutomationPackageManagerOS.createIsolatedAutomationPackageManagerOS(
                                    new ObjectId(),
                                    context.require(FunctionTypeRegistry.class),
                                    functionAccessor,
                                    resourceManager,
                                    new AutomationPackageReaderOS())
                    )
            );
            functionAccessor.save(localFunctions);
        }
    }

    private List<Function> getFunctionsFromAutomationPackage(ResourceManager resourceManager, AutomationPackageManager automationPackageManager) {
        ArrayList<Function> res = new ArrayList<>();

        // the automation package should be found in current classloader
        AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(this.getClass().getClassLoader());
        if (automationPackageArchive.hasAutomationPackageDescriptor()) {
            try {
                AutomationPackageKeywordsAttributesApplier attributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);
                AutomationPackageContent automationPackageContent = automationPackageManager.getPackageReader().readAutomationPackage(automationPackageArchive, true);
                if (automationPackageContent != null) {
                    res.addAll(attributesApplier.applySpecialAttributesToKeyword(automationPackageContent.getKeywords(), automationPackageArchive, null));
                }
            } catch (AutomationPackageReadingException e) {
                throw new RuntimeException("Unable to extract functions from automation package", e);
            }
        }
        return res;
    }

}
