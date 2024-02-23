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

public class AutomationPackageUpdateResult {
    private AutomationPackageUpdateStatus status;
    private ObjectId id;

    public AutomationPackageUpdateResult() {
    }

    public AutomationPackageUpdateResult(AutomationPackageUpdateStatus status, ObjectId id) {
        this.status = status;
        this.id = id;
    }

    public AutomationPackageUpdateStatus getStatus() {
        return status;
    }

    public ObjectId getId() {
        return id;
    }

    public void setStatus(AutomationPackageUpdateStatus status) {
        this.status = status;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
}
