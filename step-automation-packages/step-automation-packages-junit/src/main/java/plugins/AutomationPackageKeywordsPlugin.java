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
package plugins;

import step.automation.packages.AutomationPackageFile;
import step.automation.packages.yaml.AutomationPackageKeywordsExtractor;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Plugin(dependencies = {LocalFunctionPlugin.class} )
public class AutomationPackageKeywordsPlugin extends AbstractExecutionEnginePlugin {

    private FunctionAccessor functionAccessor;
    private FunctionTypeRegistry functionTypeRegistry;

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if(context.getOperationMode() == OperationMode.LOCAL) {
            functionAccessor = context.require(FunctionAccessor.class);
            functionTypeRegistry = context.require(FunctionTypeRegistry.class);

            List<Function> localFunctions = getFunctionsFromAutomationPackage();
            functionAccessor.save(localFunctions);
        }
    }

    public List<Function> getFunctionsFromAutomationPackage() {
        AutomationPackageKeywordsExtractor keywordsExtractor = new AutomationPackageKeywordsExtractor();

        // TODO: lookup for automation.yaml and extract keywords
        File descriptor = null;
        for (String metadataFile : AutomationPackageFile.METADATA_FILES) {
            descriptor = new File(metadataFile);
            if(descriptor.exists()){
                break;
            }
        }
        if(descriptor != null){
            //  ...
//            keywordsExtractor.extractKeywordsFromAutomationPackage()
        }
        return new ArrayList<>();
    }
}
