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

import jakarta.json.JsonObject;
import step.automation.packages.StagingAutomationPackageContext;
import step.core.yaml.YamlModelUtils;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.AbstractYamlModel;
import step.core.yaml.YamlFieldCustomCopy;
import step.functions.Function;
import step.jsonschema.JsonSchema;
import step.jsonschema.JsonSchemaDefaultValueProvider;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class AbstractYamlFunction<T extends Function> extends AbstractYamlModel implements AutomationPackageContextual<T> {

    @YamlFieldCustomCopy
    @JsonSchema(defaultProvider = DefaultYamlFunctionNameProvider.class)
    private String name;

    private DynamicValue<Integer> callTimeout;
    private JsonObject schema;

    private boolean executeLocally;

    @YamlFieldCustomCopy
    private Map<String, String> routing;

    private boolean useCustomTemplate=false;

    private String description;

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

    public Map<String, String> getRouting() {
        return routing;
    }

    public void setRouting(Map<String, String> routing) {
        this.routing = routing;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected void fillDeclaredFields(T res, StagingAutomationPackageContext context){
        res.addAttribute(AbstractOrganizableObject.NAME, this.getName());
        copyFieldsToObject(res, true);
    }

    protected abstract T createFunctionInstance();

    @Override
    public T applyAutomationPackageContext(StagingAutomationPackageContext context) {
        T res = createFunctionInstance();
        fillDeclaredFields(res, context);
        return res;
    }

    public static class DefaultYamlFunctionNameProvider implements JsonSchemaDefaultValueProvider {

        public DefaultYamlFunctionNameProvider() {
        }

        @Override
        public String getDefaultValue(Class<?> objectClass, Field field) {
            return YamlModelUtils.getEntityNameByClass(objectClass);
        }
    }
}
