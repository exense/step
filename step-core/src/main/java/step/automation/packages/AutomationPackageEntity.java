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

import org.bson.types.ObjectId;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.entities.Entity;

import java.util.Map;

public class AutomationPackageEntity extends Entity<AutomationPackage, AutomationPackageAccessor> {

    public static final String AUTOMATION_PACKAGE_ID = "automationPackageId";
    public static final String AUTOMATION_PACKAGE_FILE_NAME = "automationPackageFileName";

    public static final String entityName = "automationPackages";

    public AutomationPackageEntity(AutomationPackageAccessor accessor) {
        super(entityName, accessor, AutomationPackage.class);
    }

    public static Map<String, String> getAutomationPackageIdCriteria(ObjectId automationPackageId) {
        return Map.of(getAutomationPackageTrackingField(), automationPackageId.toString());
    }

    public static String getAutomationPackageTrackingField() {
        return "customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID;
    }

    public static IndexField getIndexField() {
        return new IndexField(getAutomationPackageTrackingField(), Order.ASC, String.class);
    }
}
