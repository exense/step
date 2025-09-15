/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.automation.packages;

import step.attachments.FileResolver;
import step.framework.server.Session;
import step.resources.ResourceManager;

import java.util.function.BiFunction;

public class AutomationPackageTableTransformer implements BiFunction<AutomationPackageTableRecord, Session<?>, AutomationPackageTableRecord> {

    private final ResourceManager resourceManager;
    private final FileResolver fileResolver;

    public AutomationPackageTableTransformer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        fileResolver = new FileResolver(resourceManager);
    }

    @Override
    public AutomationPackageTableRecord apply(AutomationPackageTableRecord automationPackage, Session<?> session) {
        if(automationPackage == null){
            return null;
        }

        // extend the original object with information about linked resources
        if(FileResolver.isResource(automationPackage.getAutomationPackageResource())){
            String resourceId = FileResolver.resolveResourceId(automationPackage.getAutomationPackageResource());
            automationPackage.setAutomationPackageResourceObj(resourceManager.getResource(resourceId));
        }

        if(FileResolver.isResource(automationPackage.getKeywordLibraryResource())){
            String resourceId = FileResolver.resolveResourceId(automationPackage.getKeywordLibraryResource());
            automationPackage.setKeywordLibraryResourceObj(resourceManager.getResource(resourceId));
        }

        return automationPackage;
    }

}
