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
package step.core.controller.settings;

import java.util.HashMap;
import java.util.Map;
import step.core.accessors.AbstractIdentifiableObject;

public class AbstractScopeObject extends AbstractIdentifiableObject {

    private String settingId;
    private Map<String,String> scope = new HashMap<>();

    public AbstractScopeObject() {
    }

    public void addScope(String key, String value) {
        if (scope == null) {
            scope = new HashMap<>();
        }
        scope.put(key, value);
    }

    public String getSettingId() {
        return settingId;
    }

    public void setSettingId(String settingId) {
        this.settingId = settingId;
    }

    public Map<String, String> getScope() {
        return scope;
    }

    public void setScope(Map<String, String> scope) {
        this.scope = scope;
    }
}
