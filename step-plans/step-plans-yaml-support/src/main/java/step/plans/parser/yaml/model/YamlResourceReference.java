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
package step.plans.parser.yaml.model;

import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;
import step.plans.parser.yaml.schema.YamlResourceReferenceJsonSchemaHelper;

@JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + YamlResourceReferenceJsonSchemaHelper.RESOURCE_REFERENCE_DEF)
public class YamlResourceReference {
    protected String simpleString;
    protected String resourceId;

    public YamlResourceReference() {
    }

    public YamlResourceReference(String simpleString, String resourceId) {
        this.simpleString = simpleString;
        this.resourceId = resourceId;
    }

    public DynamicValue<String> toDynamicValue(){
        if(simpleString != null && !simpleString.isEmpty()){
            return new DynamicValue<>(simpleString);
        } else if (resourceId != null && !resourceId.isEmpty()){
            return new DynamicValue<>(FileResolver.RESOURCE_PREFIX + resourceId);
        } else {
            return new DynamicValue<>();
        }
    }

    public static YamlResourceReference fromDynamicValue(DynamicValue<String> res){
        // TODO: now we only support file resources resource ids in plans
        return new YamlResourceReference(null, res.getValue().replaceFirst(FileResolver.RESOURCE_PREFIX, ""));
    }

    public String getSimpleString() {
        return simpleString;
    }

    public void setSimpleString(String simpleString) {
        this.simpleString = simpleString;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public boolean isEmpty() {
        return this.resourceId == null && this.simpleString == null;
    }
}
