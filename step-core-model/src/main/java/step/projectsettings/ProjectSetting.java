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
package step.projectsettings;

import step.core.EncryptedTrackedObject;
import step.unique.EntityWithUniqueAttributes;

public class ProjectSetting extends EncryptedTrackedObject implements EntityWithUniqueAttributes {

    public static final String ENTITY_NAME = "projectsettings";
    public static final String KEY_FIELD_NAME = "key";

    protected String value;
    protected String description;

    public ProjectSetting() {
        super();
    }

    public ProjectSetting(String key, String value, String description) {
        super();
        this.value = value;
        this.key = key;
        this.description = description;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getKeyFieldName() {
        return KEY_FIELD_NAME;
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getScopeEntity() {
        return null;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ProjectSetting [key=" + key + "]";
    }

}
