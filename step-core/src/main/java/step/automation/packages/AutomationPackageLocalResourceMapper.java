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
package step.automation.packages;

import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicValue;

/**
 * The editor counterpart of {@link AutomationPackageResourceMapper}: where that one maps the plain
 * relative path held by the YAML descriptor to the {@code apResource:<apId>:} reference of a deployed
 * package, this one maps it to {@code apResource:local:<relativePath>} - the form the entities carry
 * <b>in memory</b> while the package is being edited.
 * <p>
 * It is the {@code AutomationPackageResourceMapper} of the staging context that
 * {@code AutomationPackageReader.getAutomationPackageYamlFragmentManager} builds for the editor, which
 * is what makes every keyword plugin and the data sources of a plan produce the local form without
 * knowing anything about the editor.
 * <p>
 * The descriptor itself is not touched: the yaml models strip the prefix again on the way back to
 * YAML. Doing so buys three things a plain relative path cannot give:
 * the reference is validated against the automation package root ({@link FileResolver#normalizeApRelativePath}),
 * every consumer that branches on the reference format ({@code ExcelFileLookup},
 * {@code AbstractScriptFunctionType}, ...) takes the same branch as for a deployed package, and the
 * value is self-describing for the clients.
 *
 * @see FileResolver#LOCAL_AP_ID
 */
public class AutomationPackageLocalResourceMapper extends AutomationPackageResourceMapper {

    @Override
    public String applyUniqueResourceReference(String resourceReference,
                                               StagingAutomationPackageContext context) {
        return applyResourceReference(resourceReference, context);
    }

    /**
     * The inverse: what an entity carries as {@code apResource:local:<relativePath>} is written back to
     * the descriptor as {@code <relativePath>}. Anything else - a {@code resource:<id>}, a reference to
     * a deployed package, a path - is returned untouched.
     * <p>
     * Called by the {@code setDeclaredFieldsFromObject} of each keyword plugin. The data sources of a
     * plan do not go through here: {@code YamlResourceReference} maps them on the way to yaml, for
     * every writer of a plan rather than for the editor alone.
     *
     * @return the descriptor form, {@code null} <b>only</b> for a {@code null} reference - a caller
     * that has already excluded null needs no further check, which is what {@code
     * YamlK6Function.descriptorPath} relies on to map the separators of the result
     */
    public static String toDescriptorReference(String reference) {
        return FileResolver.isLocalApResource(reference) ? FileResolver.extractApRelativePath(reference) : reference;
    }

    /**
     * @return the descriptor form of a reference held by a {@link DynamicValue}. An absent reference
     * becomes an <b>empty</b> value rather than none: a yaml model serialized with {@code NON_DEFAULT}
     * inclusion compares what it holds against its own default through {@link DynamicValue#equals},
     * which reads both values, so a value holding nothing at all cannot be written. A dynamic
     * expression is returned as it is - there is no path in it to map back.
     */
    public static DynamicValue<String> toDescriptorReference(DynamicValue<String> reference) {
        if (reference != null && reference.isDynamic()) {
            return reference;
        }
        String value = reference == null ? null : reference.getValue();
        return new DynamicValue<>(value == null ? "" : toDescriptorReference(value));
    }

    /**
     * Unlike {@link AutomationPackageResourceMapper#applyResourceReference}, which refuses a
     * hand-written {@code apResource:} reference, this one lets it through. Refusing would mean
     * refusing to <i>open</i> the package for editing, leaving the user nowhere to fix it; and the
     * editor needs no rescue, since the reference is written back as a plain path on the next save.
     * Opening the package and saving is therefore one way to act on the deployment error.
     *
     * @return {@code null} for an absent reference, an already prefixed reference untouched (a
     * hand-written {@code resource:<id>} keeps working, and the mapping stays idempotent), and
     * {@code apResource:local:<normalisedPath>} for a relative path
     * @throws IllegalArgumentException if the path escapes the automation package root - an
     *                                  automation package is self-contained, so this is a broken
     *                                  descriptor rather than an exotic reference
     */
    @Override
    public String applyResourceReference(String resourceReference,
                                         StagingAutomationPackageContext context) {
        if (resourceReference == null || resourceReference.isEmpty()) {
            return null;
        }
        if (FileResolver.isResource(resourceReference) || FileResolver.isApResource(resourceReference)) {
            return resourceReference;
        }
        return FileResolver.createPathForLocalApResource(FileResolver.normalizeApRelativePath(resourceReference));
    }

}
