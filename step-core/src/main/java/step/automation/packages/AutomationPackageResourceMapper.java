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
 * Maps the resource references of an automation package at deploy time, from the form its descriptor
 * holds - a path relative to the package root - into an {@code apResource:<apId>:<relativePath>}
 * reference, resolved on the fly from the package archive at execution time (see
 * {@link ApResourceMaterializer}).
 * <p>
 * Nothing is uploaded or copied: a file embedded in an automation package stays in it. This class was
 * called {@code AutomationPackageResourceUploader} while it did copy each file into a Step
 * {@code Resource} of its own - which is precisely what the {@code apResource:} scheme replaced.
 * <p>
 * The editor maps to its own form instead - see {@link AutomationPackageLocalResourceMapper}, which
 * overrides both public methods.
 */
public class AutomationPackageResourceMapper {

    private final Map<String, String> uniqueResourceReferences = new ConcurrentHashMap<>();

    public String applyUniqueResourceReference(String resourceReference,
                                               StagingAutomationPackageContext context) {
        return uniqueResourceReferences.computeIfAbsent(resourceReference, key -> applyResourceReference(resourceReference, context));
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
                                         StagingAutomationPackageContext context) {
        if (resourceReference == null || resourceReference.isEmpty()) {
            return null;
        }
        if (FileResolver.isLocalApResource(resourceReference)) {
            // The editor form never reaches this far: the YAML holds plain relative paths, and
            // AutomationPackageYamlFragmentManager.save strips the prefix before writing. Getting one
            // here means an entity was staged straight out of the editor, and returning it untouched
            // as the branch below does would deploy a reference nothing can resolve.
            throw new RuntimeException("The reference " + resourceReference + " of the automation package "
                + context.getAutomationPackage().getId().toHexString() + " is an editor-local reference and "
                + "cannot be deployed. This is an internal error - such references must be resolved to a "
                + "relative path before the package is read.");
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
