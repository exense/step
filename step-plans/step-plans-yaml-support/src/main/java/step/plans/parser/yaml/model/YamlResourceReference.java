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
import step.plans.parser.yaml.schema.YamlResourceReferenceSchemaDefinitionCreator;

/**
 * The reference to some static resource (file) in yaml.
 */
@JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + YamlResourceReferenceSchemaDefinitionCreator.RESOURCE_REFERENCE_DEF)
public class YamlResourceReference {

    /**
     * The simple reference (file location in classpath)
     */
    protected String simpleString;

    /**
     * The reference on the existing resource in step via resource id
     */
    protected String resourceId;

    public YamlResourceReference() {
    }

    public YamlResourceReference(String simpleString, String resourceId) {
        this.simpleString = simpleString;
        this.resourceId = resourceId;
    }

    public DynamicValue<String> toDynamicValue() {
        if (simpleString != null && !simpleString.isEmpty()) {
            return new DynamicValue<>(simpleString);
        } else if (resourceId != null && !resourceId.isEmpty()) {
            return new DynamicValue<>(FileResolver.RESOURCE_PREFIX + resourceId);
        } else {
            return new DynamicValue<>();
        }
    }

    /**
     * The inverse of {@link #toDynamicValue()}: a {@code resource:<id>} comes back as a resource id, and
     * anything else - a path relative to the automation package above all - as the simple reference it
     * was written as. Labelling every value as a resource id, as this did, turned the path of a data
     * source into an id on the way back to the descriptor.
     * <p>
     * An {@code apResource:<apId>:<relativePath>} reference comes back as its relative path, which is
     * the form yaml has for it - {@code apId} belongs to the entity in memory, not to a descriptor, and
     * is put back by {@code AutomationPackageResourceMapper} from the package being read. This is what
     * makes the reference of a data source render as a path <b>wherever a plan is written as yaml</b>:
     * the automation package editor, and {@code GET /plans/{id}/yaml} on a controller, which would
     * otherwise hand the user an id of that server's database.
     * <p>
     * A reference to a <i>different</i> automation package is written as a relative path too, and so
     * comes back pointing at the package that carries the plan. Such a reference can only be
     * hand-written - nothing produces one - and yaml has no form for it.
     */
    public static YamlResourceReference fromDynamicValue(DynamicValue<String> res) {
        String reference = res.getValue();
        if (FileResolver.isResource(reference)) {
            return new YamlResourceReference(null, reference.substring(FileResolver.RESOURCE_PREFIX.length()));
        }
        if (FileResolver.isApResource(reference)) {
            return new YamlResourceReference(FileResolver.extractApRelativePath(reference), null);
        }
        return new YamlResourceReference(reference, null);
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
