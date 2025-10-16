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

import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;

public class AutomationPackageRedeployException extends AutomationPackageManagerException {

    private final List<ObjectId> failedApsId;

    public AutomationPackageRedeployException(List<ObjectId> failedApsIds) {
        super(createErrorMessage(failedApsIds));
        this.failedApsId = failedApsIds;
    }

    private static String createErrorMessage(List<ObjectId> failedApsIds){
        return "Unable to reupload the automation packages: " + failedApsIds.stream().map(ObjectId::toHexString).collect(Collectors.toList());
    }

    public List<ObjectId> getFailedApsId() {
        return failedApsId;
    }
}
