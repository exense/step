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

import step.commons.activation.Expression;
import step.core.EncryptedTrackedObject;
import step.core.dynamicbeans.DynamicValue;
import step.multitenancy.TenantUniqueEntity;

public class ProjectSetting extends EncryptedTrackedObject implements TenantUniqueEntity<String> {

    public static final String ENTITY_NAME = "projectsettings";

    protected String description;

    protected Expression activationExpression;

    protected Integer priority;


    /**
     * When running with an encryption manager, the value of protected
     * {@link ProjectSetting}s is encrypted and the encrypted value is stored into this
     * field
     */
    protected String encryptedValue;

    public ProjectSetting() {
        super();
    }

    public ProjectSetting(Expression activationExpression, String key, String value, String description) {
        super();
        this.key = key;
        this.value = new DynamicValue<>(value);
        this.description = description;
        this.activationExpression = activationExpression;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

    @Override
    public Expression getActivationExpression() {
        return activationExpression;
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    public void setActivationExpression(Expression activationExpression) {
        this.activationExpression = activationExpression;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    @Override
    public String toString() {
        return "ProjectSetting [key=" + key + "]";
    }


}
