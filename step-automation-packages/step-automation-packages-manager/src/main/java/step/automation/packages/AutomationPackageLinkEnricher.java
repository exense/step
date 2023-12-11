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

import step.core.accessors.AbstractIdentifiableObject;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;

import java.util.HashMap;
import java.util.Map;

public class AutomationPackageLinkEnricher implements ObjectEnricher {

    private String automationPackageId;

    public AutomationPackageLinkEnricher(String automationPackageId) {
        this.automationPackageId = automationPackageId;
    }

    @Override
    public Map<String, String> getAdditionalAttributes() {
        return new HashMap<>();
    }

    @Override
    public void accept(EnricheableObject enricheableObject) {
        if (enricheableObject instanceof AbstractIdentifiableObject) {
            ((AbstractIdentifiableObject) enricheableObject).addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID, automationPackageId);
        }
    }
}
