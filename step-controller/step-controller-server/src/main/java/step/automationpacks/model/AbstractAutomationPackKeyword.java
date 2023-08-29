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
package step.automationpacks.model;

import jakarta.json.JsonObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.json.JsonProviderCache;

import java.util.Map;

public abstract class AbstractAutomationPackKeyword {

    private DynamicValue<String> name;

    protected DynamicValue<Integer> callTimeout = new DynamicValue<>(180000);
    protected JsonObject schema = JsonProviderCache.createObjectBuilder().build();

    protected boolean executeLocally;
    protected Map<String, String> tokenSelectionCriteria;

    protected boolean managed;

    protected boolean useCustomTemplate=false;

    protected String description;

    public DynamicValue<String> getName() {
        return name;
    }

    public void setName(DynamicValue<String> name) {
        this.name = name;
    }

    public DynamicValue<Integer> getCallTimeout() {
        return callTimeout;
    }

    public void setCallTimeout(DynamicValue<Integer> callTimeout) {
        this.callTimeout = callTimeout;
    }

    public JsonObject getSchema() {
        return schema;
    }

    public void setSchema(JsonObject schema) {
        this.schema = schema;
    }

    public boolean isExecuteLocally() {
        return executeLocally;
    }

    public void setExecuteLocally(boolean executeLocally) {
        this.executeLocally = executeLocally;
    }

    public Map<String, String> getTokenSelectionCriteria() {
        return tokenSelectionCriteria;
    }

    public void setTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
        this.tokenSelectionCriteria = tokenSelectionCriteria;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public boolean isUseCustomTemplate() {
        return useCustomTemplate;
    }

    public void setUseCustomTemplate(boolean useCustomTemplate) {
        this.useCustomTemplate = useCustomTemplate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
