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

public class ConflictingAutomationPackages {
    private List<ObjectId> apWithSameOrigin = null;
    private List<ObjectId> apWithSameKeywordLib = null;

    public ConflictingAutomationPackages() {
    }

    public List<ObjectId> getApWithSameOrigin() {
        return apWithSameOrigin;
    }

    public void setApWithSameOrigin(List<ObjectId> apWithSameOrigin) {
        this.apWithSameOrigin = apWithSameOrigin;
    }

    public List<ObjectId> getApWithSameKeywordLib() {
        return apWithSameKeywordLib;
    }

    public void setApWithSameKeywordLib(List<ObjectId> apWithSameKeywordLib) {
        this.apWithSameKeywordLib = apWithSameKeywordLib;
    }

    public boolean apWithSameOriginExists(){
        return apWithSameOrigin != null && !apWithSameOrigin.isEmpty();
    }

    public boolean apWithSameKeywordLibExists(){
        return apWithSameKeywordLib != null && !apWithSameKeywordLib.isEmpty();
    }

}
