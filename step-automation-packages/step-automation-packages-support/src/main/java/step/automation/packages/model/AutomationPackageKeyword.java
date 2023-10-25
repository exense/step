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
package step.automation.packages.model;

import step.functions.Function;

import java.util.Map;

public class AutomationPackageKeyword {

    private final Function draftKeyword;
    private final Map<String, Object> specialAttributes;

    /**
     * @param draftKeyword partially filled keywords with data parsed from automation package descriptor
     * @param specialAttributes additional attributes to be separately applied to the draftKeywords (like the links to resource files,
     *                          which should be stored in databased as resources)
     */
    public AutomationPackageKeyword(Function draftKeyword, Map<String, Object> specialAttributes) {
        this.draftKeyword = draftKeyword;
        this.specialAttributes = specialAttributes;
    }

    public Function getDraftKeyword() {
        return draftKeyword;
    }

    public Map<String, Object> getSpecialAttributes() {
        return specialAttributes;
    }
}
