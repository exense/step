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

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rewrites the resource references of an automation package at deploy time. It rewrites a plain
 * archive-relative reference into an {@code apResource:<apId>:<relativePath>} reference, which is
 * resolved on the fly from the automation package archive at execution time (see
 * {@link ApResourceMaterializer}).
 * <p>
 * The IDE / local mode keeps the plain reference untouched via
 * {@link AutomationPackageLocalResourceMapper}, which overrides both public methods.
 */
public class AutomationPackageResourceUploader {

    private final Map<String, String> uniqueResourceReferences = new ConcurrentHashMap<>();

    public String applyUniqueResourceReference(String resourceReference,
                                               String resourceType,
                                               StagingAutomationPackageContext context) {
        return uniqueResourceReferences.computeIfAbsent(resourceReference, key -> applyResourceReference(resourceReference, resourceType, context));
    }

    /**
     * Rewrites {@code resourceReference} into an {@code apResource:} reference.
     * <ul>
     *     <li>{@code null} / empty → {@code null} (no reference).</li>
     *     <li>An already-absolute reference ({@code resource:} from a pre-change package, or an
     *     {@code apResource:} one) is returned untouched — back-compatibility and idempotency.</li>
     *     <li>A plain archive-relative path is validated against the archive (a missing entry fails
     *     now, at deploy time, not mid-execution) and rewritten to
     *     {@code apResource:<apId>:<normalisedRelativePath>}.</li>
     * </ul>
     */
    public String applyResourceReference(String resourceReference,
                                         String resourceType,
                                         StagingAutomationPackageContext context) {
        if (resourceReference == null || resourceReference.isEmpty()) {
            return null;
        }
        if (FileResolver.isResource(resourceReference) || FileResolver.isApResource(resourceReference)) {
            return resourceReference;
        }
        // Normalise once so deploy-time validation and runtime materialisation use the exact same path.
        String relativePath = FileResolver.normalizeApRelativePath(resourceReference);
        URL resourceUrl = context.getAutomationPackageArchive().getResource(relativePath);
        if (resourceUrl == null) {
            throw new RuntimeException("Resource not found in automation package: " + resourceReference);
        }
        String apId = context.getAutomationPackage().getId().toHexString();
        return FileResolver.createPathForApResource(apId, relativePath);
    }
}
