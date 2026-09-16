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
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * Replaces each staged keyword package, according to the configured mode.
 */
public class KeywordPackageMigrationExecutor {

    /** Back-reference from a keyword to the keyword package that deployed it. */
    static final String FUNCTION_PACKAGE_ID_CUSTOM_FIELD = "functionPackageId";

    private static final Logger logger = LoggerFactory.getLogger(KeywordPackageMigrationExecutor.class);

    private static final String MIGRATION_ACTOR = "keyword-package-migration";

    static final String UNNAMED_PACKAGE = "unnamed";

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
        this.staging = Objects.requireNonNull(staging, "The staging must not be null");
        this.functionAccessor = Objects.requireNonNull(functionAccessor, "The functionAccessor must not be null");
        this.automationPackageAccessor = Objects.requireNonNull(automationPackageAccessor, "The automationPackageAccessor must not be null");
        this.automationPackageManager = Objects.requireNonNull(automationPackageManager, "The automationPackageManager must not be null");
        this.resourceManager = Objects.requireNonNull(resourceManager, "The resourceManager must not be null");
        this.objectHookRegistry = Objects.requireNonNull(objectHookRegistry, "The objectHookRegistry must not be null");
        this.mode = Objects.requireNonNull(mode, "The mode must not be null");
        this.classifier = new KeywordPackageClassifier();
    }

    public void run() {
        List<StagedKeywordPackage> staged = staging.find(Filters.empty(), null, null, null, 0).toList();
        if (staged.isEmpty()) {
            //non-existent collections are silently recreated on any access, so we have to clean it up here
            staging.drop();
            return;
        }

        logger.info("Replacing {} keyword package(s) in {} mode.", staged.size(), mode.getPropertyValue());
        int deployed = 0;
        List<String> failures = new ArrayList<>();

        for (StagedKeywordPackage stagedPackage : staged) {
            KeywordPackageMigrationEligibility eligibility = classifier.classify(stagedPackage);
            try {
                if (process(stagedPackage, eligibility)) {
                    deployed++;
                }
            } catch (Exception e) {
                failures.add(stagedPackage.describe());
                logger.error("Unable to migrate the keyword package {}. It has been removed without a "
                        + "replacement.", stagedPackage.describe(), e);
            } finally {
                // A failed package is not retried, so its record it always deleted.
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
     * Decides what happens to one keyword package, in this order:
     * <ul>
     *     <li>{@link KeywordPackageMigrationEligibility#EMBEDDED}, or any package in
     *     {@link KeywordPackageMigrationMode#DELETE} mode: keywords and owned resources are removed</li>
     *     <li>{@link KeywordPackageMigrationMode#DETACH} mode: the keywords are kept and unlinked, an
     *     {@link KeywordPackageMigrationEligibility#INCOMPLETE} package included, since that mode exists
     *     to leave keywords untouched</li>
     *     <li>{@link KeywordPackageMigrationEligibility#INCOMPLETE}: the keywords are removed, the
     *     resources kept</li>
     *     <li>{@link KeywordPackageMigrationEligibility#MIGRATABLE} in
     *     {@link KeywordPackageMigrationMode#MIGRATE} mode, the only combination left: an automation
     *     package is deployed</li>
     * </ul>
     *
     * @return true if an automation package was deployed for this keyword package
     */
    private boolean process(StagedKeywordPackage staged, KeywordPackageMigrationEligibility eligibility) throws Exception {
        boolean deployed = false;
        if (KeywordPackageMigrationEligibility.EMBEDDED.equals(eligibility) || mode == KeywordPackageMigrationMode.DELETE) {
            // Embedded packages are recreated by the embedded automation package feature, so they are
            // deleted whatever the mode.
            deleteKeywords(staged);
            deleteIfIsResource(staged.getPackageLocation());
            deleteIfIsResource(staged.getPackageLibrariesLocation());
        } else if (mode == KeywordPackageMigrationMode.DETACH) {
            detachKeywords(staged);
        } else if (KeywordPackageMigrationEligibility.INCOMPLETE.equals(eligibility)) {
            // The resources are kept: the missing one may be only the archive or only the libraries,
            // and the other can still be a valid resource shared with another package.
            logger.warn("The keyword package {} points at an archive or libraries that cannot be read, "
                    + "its keywords cannot be executed. The package and its keywords are removed "
                    + "without a replacement, its Step resources are left in place.", staged.describe());
            deleteKeywords(staged);
        } else if (KeywordPackageMigrationEligibility.MIGRATABLE.equals(eligibility)) {
            deploy(staged);
            deployed = true;
        } else {
            // Unreachable, unless an eligibility is added without being handled here.
            throw new IllegalStateException("Unsupported eligibility " + eligibility + " in "
                    + mode.getPropertyValue() + " mode for the keyword package " + staged.describe() + ".");
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

        String archiveLocation = staged.getPackageLocation();
        String librariesLocation = staged.getPackageLibrariesLocation();
        // The manager reads the streams during the deployment but does not close them
        try (InputStream archiveContent = openUnlessResource(archiveLocation);
             InputStream librariesContent = librariesLocation == null ? null : openUnlessResource(librariesLocation)) {
            AutomationPackageFileSource archiveSource = sourceFor(archiveLocation, archiveContent,
                    ResourceManager.RESOURCE_TYPE_AP)
                    .withArchiveName(resolveUniqueName(staged, objectPredicate));
            AutomationPackageFileSource librariesSource = librariesLocation == null ? null
                    : sourceFor(librariesLocation, librariesContent, ResourceManager.RESOURCE_TYPE_AP_LIBRARY);

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
    }

    /**
     * @return the content of a filesystem location, or null for a resource, which is reused in place
     */
    private InputStream openUnlessResource(String location) throws IOException {
        InputStream content;
        if (FileResolver.isResource(location)) {
            content = null;
        } else {
            content = new FileInputStream(location);
        }
        return content;
    }

    /**
     * An existing resource is reused, re-typed so that the automation package paths accept it. A
     * filesystem path has no resource yet, so the manager creates one from the content.
     */
    private AutomationPackageFileSource sourceFor(String location, InputStream content, String resourceType) throws IOException {
        AutomationPackageFileSource source;
        if (FileResolver.isResource(location)) {
            // Raises ResourceMissingException when the resource is gone, which drops the package.
            String resourceId = FileResolver.resolveResourceId(location);
            resourceManager.changeResourceType(resourceId, resourceType);
            source = AutomationPackageFileSource.withResourceId(resourceId);
        } else {
            source = AutomationPackageFileSource.withInputStream(content, new File(location).getName());
        }
        return source;
    }

    /**
     * Appends a counter when the name is already taken. Two keyword packages built from identically
     * named archives are legal, but an automation package name must be unique within its scope.
     *
     * @param objectPredicate the scope the deployment will use, so a name taken in another project is
     *                        not a collision
     */
    private String resolveUniqueName(StagedKeywordPackage staged, ObjectPredicate objectPredicate) {
        // Unlikely to happen, but default to "unnamed" if the name is not set
        String baseName = Objects.requireNonNullElse(staged.getAttribute(AbstractOrganizableObject.NAME), UNNAMED_PACKAGE);
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
        return StreamSupport
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
     * Removed through the accessor directly and not {@code FunctionManager.deleteFunction},
     * which would deletes the linked resource of the keyword — the resource being reused
     * by the migration
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
    private void deleteIfIsResource(String location) {
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
