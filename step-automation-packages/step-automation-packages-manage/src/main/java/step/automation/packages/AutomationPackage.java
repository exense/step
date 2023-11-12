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

import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;

import java.util.Map;

public class AutomationPackage extends AbstractOrganizableObject implements EnricheableObject {

    // TODO: token selection criteria for keywords?
    public static final String TRACKING_FIELD = "tracking";

    protected Map<String, String> packageAttributes;

    /**
     * @return the additional attributes that have to be added to the attributes of the functions contained in this package
     */
    public Map<String, String> getPackageAttributes() {
        return packageAttributes;
    }

    public void setPackageAttributes(Map<String, String> packageAttributes) {
        this.packageAttributes = packageAttributes;
    }

}
