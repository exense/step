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

public class AutomationPackageCollisionException extends AutomationPackageManagerException {

    protected List<ObjectId> automationPackagesWithSameOrigin;
    protected List<ObjectId> automationPackagesWithSameKeywordLib;

    public AutomationPackageCollisionException(List<ObjectId> automationPackagesWithSameOrigin, List<ObjectId> automationPackagesWithSameKeywordLib){
        super(createErrorMessage(automationPackagesWithSameOrigin, automationPackagesWithSameKeywordLib));
        this.automationPackagesWithSameOrigin = automationPackagesWithSameOrigin;
        this.automationPackagesWithSameKeywordLib = automationPackagesWithSameKeywordLib;
    }

    public static String createErrorMessage(List<ObjectId> automationPackagesWithSameOrigin, List<ObjectId> automationPackagesWithSameKeywordLib) {
        String res = "";
        boolean newLineRequired = false;
        if (automationPackagesWithSameOrigin != null && !automationPackagesWithSameOrigin.isEmpty()) {
            newLineRequired = true;
            res += "Automation packages with same origin: " + automationPackagesWithSameOrigin.stream().map(ObjectId::toHexString).collect(Collectors.joining(","));
        }
        if (automationPackagesWithSameKeywordLib != null && !automationPackagesWithSameKeywordLib.isEmpty()) {
            if (newLineRequired) {
                res += "\n";
            } else {
                newLineRequired = true;
            }
            res += "Automation packages with same keyword lib: " + automationPackagesWithSameKeywordLib.stream().map(ObjectId::toHexString).collect(Collectors.joining(","));
        }
        if (!res.isBlank()) {
            res += "\n. Use the option 'allowUpdateOfOtherPackages' to allow the update. All automation packages referencing the same artefacts with be updated in the process.";
        }
        return res;
    }

    public List<ObjectId> getAutomationPackagesWithSameOrigin() {
        return automationPackagesWithSameOrigin;
    }

    public List<ObjectId> getAutomationPackagesWithSameKeywordLib() {
        return automationPackagesWithSameKeywordLib;
    }
}
