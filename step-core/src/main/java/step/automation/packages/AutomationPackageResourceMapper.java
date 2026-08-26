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
import step.core.plans.Plan;

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
     * Applies {@link #applyResourceReference} to every resource reference of a plan. Unlike a keyword,
     * whose plugin maps its own fields, a plan holds its references inside its artefact tree.
     * <p>
     * There is deliberately no inverse of this: a plan is written back by the yaml models, where
     * {@code YamlResourceReference} renders any {@code apResource:} reference as the path the
     * descriptor holds. That direction needs no context, so it belongs to the format - this one needs
     * the package the reference is being read for, and the archive to validate it against, so it
     * belongs here.
     */
    public void applyToPlan(Plan plan, StagingAutomationPackageContext context) {
        ResourceReferences.apply(plan.getRoot(), reference -> applyResourceReference(reference, context));
    }

    /**
     * Rewrites {@code resourceReference} into an {@code apResource:} reference.
     * <ul>
     *     <li>{@code null} / empty → {@code null} (no reference).</li>
     *     <li>An {@code apResource:} reference is <b>rejected</b>: it is what this method produces,
     *     so a descriptor holding one was written by hand.</li>
     *     <li>A {@code resource:} reference is returned untouched. This is an authored form, not a
     *     leftover: the schema declares the file of a data source as
     *     {@code oneOf: [string, {id: <string>}]}, and {@code YamlResourceReference.toDynamicValue}
     *     turns the {@code {id: ...}} form into {@code resource:<id>} before it gets here. A keyword
     *     whose script is a Step resource of the controller reaches this the same way.</li>
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
        if (FileResolver.isApResource(resourceReference)) {
            // Nothing produces one in a descriptor: this is either hand-written or a bug.
            // Deploying it as it stands would either pin the entity to another package or, for the editor form,
            // leaving a reference nothing resolves.
            throw new RuntimeException("Invalid resource reference '" + resourceReference + "' in the "
                + "automation package: an apResource: reference is built by Step when a package is "
                + "deployed and cannot be written in a descriptor. Use the path of the file relative "
                + "to the root of the automation package instead.");
        }
        if (FileResolver.isResource(resourceReference)) {
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
