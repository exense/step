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
package step.automation.packages;

/* Note: This class is used to provide a (required) AutomationPackageResourceUploader object for the
(required) StagingContext when creating an AutomationPackageYamlFragmentManager for the IDE (which
allows to edit Automation packages in place).
If you find a better name for this class, or a way to not require it, feel free to refactor.
 */
public class AutomationPackageLocalResourceMapper extends AutomationPackageResourceUploader {

    @Override
    public String applyUniqueResourceReference(String resourceReference,
                                               String resourceType,
                                               StagingAutomationPackageContext context) {
        return resourceReference;
    }

    @Override
    public String applyResourceReference(String resourceReference,
                                         String resourceType,
                                         StagingAutomationPackageContext context) {
        return resourceReference;
    }

}
