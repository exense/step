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
package step.automation.packages.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.automation.packages.AutomationPackageFileSource;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageUpdateParameter;
import step.automation.packages.AutomationPackageUpdateParameterBuilder;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.core.AbstractContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.WriteAccessValidator;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



/**
 * Replaces each staged keyword package, according to the configured mode.
 */
public class KeywordPackageMigrationExecutor {

    /** Back-reference from a keyword to the keyword package that deployed it. */
    static final String FUNCTION_PACKAGE_ID_CUSTOM_FIELD = "functionPackageId";

    private static final Logger logger = LoggerFactory.getLogger(KeywordPackageMigrationExecutor.class);

    private static final String MIGRATION_ACTOR = "keyword-package-migration";

    private final Collection<StagedKeywordPackage> staging;
    private final FunctionAccessor functionAccessor;
    private final AutomationPackageAccessor automationPackageAccessor;

    private final AutomationPackageManager automationPackageManager;
    private final ResourceManager resourceManager;
    private final ObjectHookRegistry objectHookRegistry;
    private final KeywordPackageMigrationMode mode;
    private final KeywordPackageClassifier classifier;

    public KeywordPackageMigrationExecutor(Collection<StagedKeywordPackage> staging,
                                           FunctionAccessor functionAccessor,
                                           AutomationPackageAccessor automationPackageAccessor,
                                           AutomationPackageManager automationPackageManager,
                                           ResourceManager resourceManager,
                                           ObjectHookRegistry objectHookRegistry,
                                           KeywordPackageMigrationMode mode) {
        this.staging = staging;
        this.functionAccessor = functionAccessor;
        this.automationPackageAccessor = automationPackageAccessor;
        this.automationPackageManager = automationPackageManager;
        this.resourceManager = resourceManager;
        this.objectHookRegistry = objectHookRegistry;
        this.mode = mode;
        this.classifier = new KeywordPackageClassifier();
    }

    public void run() {
        List<StagedKeywordPackage> staged = staging.find(Filters.empty(), null, null, null, 0).toList();
        if (staged.isEmpty()) {
            staging.drop();
            return;
        }

        logger.info("Replacing {} keyword package(s) in {} mode.", staged.size(), mode.getPropertyValue());
        int deployed = 0;
        List<String> failures = new ArrayList<>();

        for (StagedKeywordPackage stagedPackage : staged) {
            KeywordPackageBucket bucket = classifier.classify(stagedPackage);
            try {
                if (process(stagedPackage, bucket)) {
                    deployed++;
                }
            } catch (Exception e) {
                failures.add(stagedPackage.describe());
                logger.error("Unable to migrate the keyword package {}. It has been removed without a "
                        + "replacement.", stagedPackage.describe(), e);
            } finally {
                // A failed package is not retried, so its record goes either way.
                staging.remove(Filters.id(stagedPackage.getId()));
            }
        }

        staging.drop();

        logger.info("Keyword package migration finished in {} mode: {} of {} replaced by an automation "
                + "package.", mode.getPropertyValue(), deployed, staged.size());
        if (!failures.isEmpty()) {
            logger.error("{} keyword package(s) could not be migrated and were removed: {}",
                    failures.size(), String.join(", ", failures));
        }
    }

    /**
     * @return true if an automation package was deployed for this keyword package
     */
    private boolean process(StagedKeywordPackage staged, KeywordPackageBucket bucket) throws Exception {
        boolean deployed = false;
        if (KeywordPackageBucket.EMBEDDED.equals(bucket) || mode == KeywordPackageMigrationMode.DELETE) {
            // Embedded packages are recreated by the embedded automation package feature, so they are
            // deleted whatever the mode.
            deleteKeywords(staged);
            deleteResource(staged.getPackageLocation());
            deleteResource(staged.getPackageLibrariesLocation());
        } else if (mode == KeywordPackageMigrationMode.DETACH) {
            detachKeywords(staged);
        } else if (KeywordPackageBucket.ARCHIVE_MISSING.equals(bucket)) {
            // The resources are kept: the missing one may be only the archive or only the libraries,
            // and the other can still be a valid resource shared with another package.
            logger.warn("The keyword package {} points at an archive or libraries that cannot be read, "
                    + "its keywords cannot be executed. The package and its keywords are removed "
                    + "without a replacement, its Step resources are left in place.", staged.describe());
            deleteKeywords(staged);
        } else {
            deploy(staged);
            deployed = true;
        }
        return deployed;
    }

    // ------------------------------------------------------------- deployment

    private void deploy(StagedKeywordPackage staged) throws Exception {
        // The staged package still carries the attributes of the keyword package, so the hooks rebuild
        // the enrichment and the scope the original deployment had.
        AbstractContext enrichmentContext = new AbstractContext() {
        };
        objectHookRegistry.rebuildContext(enrichmentContext, staged);
        ObjectEnricher enricher = objectHookRegistry.getObjectEnricher(enrichmentContext);
        ObjectPredicate objectPredicate = objectHookRegistry.getObjectPredicate(enrichmentContext);

        AutomationPackageFileSource archiveSource = sourceFor(staged.getPackageLocation(),
                ResourceManager.RESOURCE_TYPE_AP)
                .withArchiveName(resolveUniqueName(staged, objectPredicate));
        AutomationPackageFileSource librariesSource = staged.getPackageLibrariesLocation() == null ? null
                : sourceFor(staged.getPackageLibrariesLocation(), ResourceManager.RESOURCE_TYPE_AP_LIBRARY);

        deleteKeywords(staged);

        AutomationPackageUpdateParameter parameters = new AutomationPackageUpdateParameterBuilder()
                .withCreateOnly()
                .withApSource(archiveSource)
                .withApLibrarySource(librariesSource)
                .withFunctionsAttributes(nullIfEmpty(staged.getPackageAttributes()))
                .withTokenSelectionCriteria(nullIfEmpty(staged.getTokenSelectionCriteria()))
                .withExecuteFunctionsLocally(staged.isExecuteLocally())
                .withEnricher(enricher)
                .withObjectPredicate(objectPredicate)
                .withWriteAccessValidator(WriteAccessValidator.NO_CHECKS_VALIDATOR)
                .withActorUser(MIGRATION_ACTOR)
                .withAsync(false)
                .build();

        AutomationPackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(parameters);
        logger.info("Migrated the keyword package {} to the automation package {} ({}).",
                staged.describe(), result.getId(), result.getStatus());
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            logger.warn("The migration of the keyword package {} reported: {}",
                    staged.describe(), String.join("; ", result.getWarnings()));
        }
    }

    /**
     * Reuses the existing resource, so that no copy of the archive is made and the
     * {@code resource:<id>} references stored elsewhere stay valid. A filesystem path has no resource
     * yet, so the manager creates one from the file.
     */
    private AutomationPackageFileSource sourceFor(String location, String resourceType) throws Exception {
        if (location.startsWith(FileResolver.RESOURCE_PREFIX)) {
            String resourceId = FileResolver.resolveResourceId(location);
            retypeResource(resourceId, resourceType);
            return AutomationPackageFileSource.withResourceId(resourceId);
        }
        File file = new File(location);
        try (InputStream content = new FileInputStream(file)) {
            return AutomationPackageFileSource.withInputStream(content, file.getName());
        }
    }

    /**
     * The deployment would accept a {@code functions}-typed resource, but the automation package
     * delete and refresh paths validate the type and reject anything outside the automation package
     * set.
     */
    private void retypeResource(String resourceId, String resourceType) throws Exception {
        // Raises ResourceMissingException when the resource is gone, which drops the package.
        Resource resource = resourceManager.getResource(resourceId);
        resource.setResourceType(resourceType);
        resourceManager.saveResource(resource);
    }

    /**
     * Appends a counter when the name is already taken. Two keyword packages built from identically
     * named archives are legal, but an automation package name must be unique within its scope.
     *
     * @param objectPredicate the scope the deployment will use, so a name taken in another project is
     *                        not a collision
     */
    private String resolveUniqueName(StagedKeywordPackage staged, ObjectPredicate objectPredicate) {
        String baseName = staged.getAttribute(AbstractOrganizableObject.NAME);
        String candidate = baseName;
        int counter = 1;
        while (isNameTaken(candidate, objectPredicate)) {
            candidate = baseName + " (" + counter++ + ")";
        }
        if (!candidate.equals(baseName)) {
            logger.warn("An automation package named '{}' already exists, the keyword package {} is "
                    + "migrated as '{}'.", baseName, staged.describe(), candidate);
        }
        return candidate;
    }

    private boolean isNameTaken(String name, ObjectPredicate objectPredicate) {
        return java.util.stream.StreamSupport
                .stream(automationPackageAccessor.findManyByAttributes(
                        Map.of(AbstractOrganizableObject.NAME, name)), false)
                .anyMatch(objectPredicate);
    }

    // --------------------------------------------------------------- keywords

    private void detachKeywords(StagedKeywordPackage staged) {
        List<Function> keywords = keywordsOf(staged);
        for (Function keyword : keywords) {
            keyword.getCustomFields().remove(FUNCTION_PACKAGE_ID_CUSTOM_FIELD);
            functionAccessor.save(keyword);
        }
        logger.info("Detached {} keyword(s) from the keyword package {}.", keywords.size(), staged.describe());
    }

    /**
     * Removed through the accessor and not {@code FunctionManager.deleteFunction}, which delegates to
     * the keyword type and deletes the linked resource of a managed keyword — the archive being reused
     * here.
     */
    private void deleteKeywords(StagedKeywordPackage staged) {
        List<Function> keywords = keywordsOf(staged);
        keywords.forEach(keyword -> functionAccessor.remove(keyword.getId()));
    }

    private List<Function> keywordsOf(StagedKeywordPackage staged) {
        return functionAccessor.findManyByCriteria(
                        Map.of("customFields." + FUNCTION_PACKAGE_ID_CUSTOM_FIELD, staged.getId().toString()))
                .collect(Collectors.toList());
    }

    /**
     * Does nothing for a package located by a filesystem path, which owns no resource.
     */
    private void deleteResource(String location) {
        if (location == null || !location.startsWith(FileResolver.RESOURCE_PREFIX)) {
            return;
        }
        try {
            resourceManager.deleteResource(FileResolver.resolveResourceId(location));
        } catch (Exception e) {
            logger.warn("Unable to delete the resource '{}'. It is left in place.", location, e);
        }
    }

    private Map<String, String> nullIfEmpty(Map<String, String> map) {
        return map == null || map.isEmpty() ? null : map;
    }
}
