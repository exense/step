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

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.InMemoryAutomationPackageAccessorImpl;
import step.automation.packages.kwlibrary.*;
import step.automation.packages.model.AutomationPackageKeyword;
import step.commons.activation.Expression;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.IndexField;
import step.core.entities.Entity;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectEnricherComposer;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.ImportResult;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.accessor.LayeredFunctionAccessor;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeException;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.SetupFunctionException;
import step.resources.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static step.automation.packages.AutomationPackageArchive.METADATA_FILES;
import static step.plans.parser.yaml.YamlPlan.PLANS_ENTITY_NAME;

public class AutomationPackageManager {

    public static final int DEFAULT_READLOCK_TIMEOUT_SECONDS = 60;

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageManager.class);

    protected final AutomationPackageAccessor automationPackageAccessor;
    protected final FunctionManager functionManager;
    protected final FunctionAccessor functionAccessor;
    private final AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider;
    protected final PlanAccessor planAccessor;
    protected final AutomationPackageReader packageReader;

    protected final ResourceManager resourceManager;
    protected final AutomationPackageHookRegistry automationPackageHookRegistry;
    protected boolean isIsolated = false;
    protected final AutomationPackageLocks automationPackageLocks;

    private Map<String, Object> extensions;

    private final ExecutorService delayedUpdateExecutor = Executors.newCachedThreadPool();

    public final AutomationPackageOperationMode operationMode;

    /**
     * The automation package manager used to store/delete automation packages. To run the automation package in isolated
     * context please use the separate in-memory automation package manager created via
     * {@link AutomationPackageManager#createIsolated(ObjectId, FunctionTypeRegistry, FunctionAccessor)}
     */
    private AutomationPackageManager(AutomationPackageOperationMode operationMode, AutomationPackageAccessor automationPackageAccessor,
                                     FunctionManager functionManager,
                                     FunctionAccessor functionAccessor,
                                     PlanAccessor planAccessor,
                                     ResourceManager resourceManager,
                                     Map<String, Object> extensions,
                                     AutomationPackageHookRegistry automationPackageHookRegistry,
                                     AutomationPackageReader packageReader,
                                     AutomationPackageLocks automationPackageLocks,
                                     AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) {
        this.automationPackageAccessor = automationPackageAccessor;

        this.functionManager = functionManager;
        this.functionAccessor = functionAccessor;
        this.mavenConfigProvider = mavenConfigProvider;
        IndexField indexField = AutomationPackageEntity.getIndexField();
        this.functionAccessor.createIndexIfNeeded(indexField);

        this.planAccessor = planAccessor;
        this.planAccessor.createIndexIfNeeded(indexField);

        this.extensions = extensions;

        this.automationPackageHookRegistry = automationPackageHookRegistry;
        this.packageReader = packageReader;
        this.resourceManager = resourceManager;
        this.automationPackageLocks = automationPackageLocks;
        this.operationMode = Objects.requireNonNull(operationMode);

        addDefaultExtensions();
    }

    private void addDefaultExtensions() {
        this.extensions.put(AutomationPackageContext.PLAN_ACCESSOR, this.planAccessor);
        this.extensions.put(AutomationPackageContext.FUNCTION_ACCESSOR, this.functionAccessor);
    }

    /**
     * Creates the local automation package manager to be used in JUnit runners
     */
    public static AutomationPackageManager createLocalAutomationPackageManager(FunctionTypeRegistry functionTypeRegistry,
                                                                               FunctionAccessor mainFunctionAccessor,
                                                                               PlanAccessor planAccessor,
                                                                               ResourceManager resourceManager,
                                                                               AutomationPackageReader reader,
                                                                               AutomationPackageHookRegistry hookRegistry) {
        Map<String, Object> extensions = new HashMap<>();
        hookRegistry.onLocalAutomationPackageManagerCreate(extensions);

        // for local AP manager we don't need to create layered accessors
        AutomationPackageManager automationPackageManager = new AutomationPackageManager(
                AutomationPackageOperationMode.LOCAL, new InMemoryAutomationPackageAccessorImpl(),
                new FunctionManagerImpl(mainFunctionAccessor, functionTypeRegistry),
                mainFunctionAccessor,
                planAccessor,
                resourceManager,
                extensions,
                hookRegistry, reader,
                new AutomationPackageLocks(DEFAULT_READLOCK_TIMEOUT_SECONDS),
                null
        );
        automationPackageManager.isIsolated = true;
        return automationPackageManager;
    }

    /**
     * Creates the automation package manager for isolated (not persisted) execution. Based on in-memory accessors
     * for plans and keywords.
     *
     * @param isolatedContextId    the unique id of isolated context (isolated execution)
     * @param functionTypeRegistry the function type registry
     * @param mainFunctionAccessor the main (persisted) accessor for keywords. it is used in read-only mode to lookup
     *                             existing keywords and override (reuse their ids) them in in-memory layer to avoid
     *                             keywords with duplicated names
     * @return the automation manager with in-memory accessors for plans and keywords
     */
    public static AutomationPackageManager createIsolatedAutomationPackageManager(ObjectId isolatedContextId,
                                                                                  FunctionTypeRegistry functionTypeRegistry,
                                                                                  FunctionAccessor mainFunctionAccessor,
                                                                                  AutomationPackageReader reader,
                                                                                  AutomationPackageHookRegistry hookRegistry) {

        ResourceManager resourceManager = new LocalResourceManagerImpl(new File("resources_" + isolatedContextId.toString()));
        InMemoryFunctionAccessorImpl inMemoryFunctionRepository = new InMemoryFunctionAccessorImpl();
        LayeredFunctionAccessor layeredFunctionAccessor = new LayeredFunctionAccessor(List.of(inMemoryFunctionRepository, mainFunctionAccessor));

        Map<String, Object> extensions = new HashMap<>();
        hookRegistry.onIsolatedAutomationPackageManagerCreate(extensions);
        AutomationPackageManager automationPackageManager = new AutomationPackageManager(
                AutomationPackageOperationMode.ISOLATED, new InMemoryAutomationPackageAccessorImpl(),
                new FunctionManagerImpl(layeredFunctionAccessor, functionTypeRegistry),
                layeredFunctionAccessor,
                new InMemoryPlanAccessor(),
                resourceManager,
                extensions,
                hookRegistry, reader,
                new AutomationPackageLocks(DEFAULT_READLOCK_TIMEOUT_SECONDS),
                null
        );
        automationPackageManager.isIsolated = true;
        return automationPackageManager;
    }

    public static AutomationPackageManager createMainAutomationPackageManager(AutomationPackageAccessor accessor,
                                                                              FunctionManager functionManager,
                                                                              FunctionAccessor functionAccessor,
                                                                              PlanAccessor planAccessor,
                                                                              ResourceManager resourceManager,
                                                                              AutomationPackageHookRegistry hookRegistry,
                                                                              AutomationPackageReader reader,
                                                                              AutomationPackageLocks locks,
                                                                              AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) {
        Map<String, Object> extensions = new HashMap<>();
        hookRegistry.onMainAutomationPackageManagerCreate(extensions);
        return new AutomationPackageManager(
                AutomationPackageOperationMode.MAIN, accessor,
                functionManager,
                functionAccessor,
                planAccessor,
                resourceManager,
                extensions,
                hookRegistry,
                reader,
                locks,
                mavenConfigProvider
        );
    }

    /**
     * Creates the automation package manager for isolated (not persisted) execution. Based on in-memory accessors
     * for plans and keywords.
     *
     * @param isolatedContextId    the unique id of isolated context (isolated execution)
     * @param functionTypeRegistry the function type registry
     * @param mainFunctionAccessor the main (persisted) accessor for keywords. it is used in read-only mode to lookup
     *                             existing keywords and override (reuse their ids) them in in-memory layer to avoid
     *                             keywords with duplicated names
     * @return the automation manager with in-memory accessors for plans and keywords
     */
    public AutomationPackageManager createIsolated(ObjectId isolatedContextId, FunctionTypeRegistry functionTypeRegistry, FunctionAccessor mainFunctionAccessor){
        return createIsolatedAutomationPackageManager(isolatedContextId, functionTypeRegistry, mainFunctionAccessor, getPackageReader(), automationPackageHookRegistry);
    }

    public AutomationPackage getAutomationPackageById(ObjectId id, ObjectPredicate objectPredicate) {
        AutomationPackage automationPackage = automationPackageAccessor.get(id);
        if (automationPackage == null) {
            throw new AutomationPackageManagerException("Automation package hasn't been found by id: " + id);
        }

        if (objectPredicate != null && !objectPredicate.test(automationPackage)) {
            // package exists, but is not accessible (linked with another product)
            throw new AutomationPackageManagerException("Automation package " + id + " is not accessible");
        }

        return automationPackage;
    }

    public AutomationPackage getAutomatonPackageById(ObjectId id, ObjectPredicate objectPredicate) {
        return this.getAutomationPackageById(id, objectPredicate);
    }

    public AutomationPackage getAutomationPackageByName(String name, ObjectPredicate objectPredicate) {
        Stream<AutomationPackage> stream = StreamSupport.stream(automationPackageAccessor.findManyByAttributes(Map.of(AbstractOrganizableObject.NAME, name)), false);
        if (objectPredicate != null) {
            stream = stream.filter(objectPredicate);
        }
        return stream.findFirst().orElse(null);
    }

    public Stream<AutomationPackage> getAllAutomationPackages(ObjectPredicate objectPredicate) {
        Stream<AutomationPackage> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(automationPackageAccessor.getAll(), Spliterator.ORDERED),
                false
        );
        if (objectPredicate != null) {
            stream = stream.filter(objectPredicate);
        }
        return stream;
    }

    public void removeAutomationPackage(ObjectId id, String actorUser, ObjectPredicate objectPredicate) {
        AutomationPackage automationPackage = getAutomationPackageById(id, objectPredicate);
        String automationPackageId = automationPackage.getId().toHexString();
        if (automationPackageLocks.tryWriteLock(automationPackageId)) {
            try {
                deleteAutomationPackageEntities(automationPackage, actorUser);
                automationPackageAccessor.remove(automationPackage.getId());
                log.info("Automation package ({}) has been removed", id);
            } finally {
                automationPackageLocks.releaseAndRemoveLock(automationPackageId);
            }
        } else {
            throw new AutomationPackageManagerException("Automation package cannot be removed while executions using it are running.");
        }
    }

    protected void deleteAutomationPackageEntities(AutomationPackage automationPackage, String actorUser) {
        deleteFunctions(automationPackage);
        deletePlans(automationPackage);
        // schedules will be deleted in deleteAdditionalData via hooks
        deleteResources(automationPackage);
        deleteAdditionalData(automationPackage, new AutomationPackageContext(automationPackage, operationMode, resourceManager, null,  null, actorUser, null, extensions));
    }

    public ObjectId createAutomationPackageFromMaven(MavenArtifactIdentifier mavenArtifactIdentifier,
                                                     String apVersion, String activationExpr,
                                                     AutomationPackageFileSource keywordLibrarySource,
                                                     ObjectEnricher enricher, ObjectPredicate objectPredicate,
                                                     String actorUser, boolean forceUpload, boolean checkForSameOrigin) {
        validateMavenConfigAndArtifactClassifier(mavenArtifactIdentifier);
        try {
            try (AutomationPackageFromMavenProvider provider = new AutomationPackageFromMavenProvider(mavenConfigProvider.getConfig(objectPredicate), mavenArtifactIdentifier)) {
                return createOrUpdateAutomationPackage(false, true, null, provider, apVersion, activationExpr, false, enricher, objectPredicate,
                        false, keywordLibrarySource, actorUser,
                        forceUpload, checkForSameOrigin).getId();
            }
        } catch (IOException ex) {
            throw new AutomationPackageManagerException("Automation package cannot be created. Caused by: " + ex.getMessage(), ex);
        }
    }

    protected void validateMavenConfigAndArtifactClassifier(MavenArtifactIdentifier mavenArtifactIdentifier) throws AutomationPackageManagerException {
        if (mavenConfigProvider == null) {
            throw new AutomationPackageManagerException("Maven config provider is not configured for automation package manager");
        }

        String commonErrorMessage = "Unable to resolve maven artifact for automation package";
        if (mavenArtifactIdentifier.getArtifactId() == null) {
            throw new AutomationPackageManagerException(commonErrorMessage + ". artifactId is undefined");
        }
        if (mavenArtifactIdentifier.getGroupId() == null) {
            throw new AutomationPackageManagerException(commonErrorMessage + ". groupId is undefined");
        }
        if (mavenArtifactIdentifier.getVersion() == null) {
            throw new AutomationPackageManagerException(commonErrorMessage + ". version is undefined");
        }
    }

    /**
     * Creates the new automation package. The exception will be thrown, if the package with the same name already exists.
     *
     * @param apSource           the content of automation package
     * @param actorUser
     * @param forceUpload
     * @param checkForSameOrigin
     * @param enricher           the enricher used to fill all stored objects (for instance, with product id for multitenant application)
     * @param objectPredicate    the filter for automation package
     * @return the id of created package
     * @throws AutomationPackageManagerException
     */
    public ObjectId createAutomationPackage(AutomationPackageFileSource apSource, String apVersion, String activationExpr,
                                            AutomationPackageFileSource keywordLibrarySource, String actorUser, boolean forceUpload,
                                            boolean checkForSameOrigin, ObjectEnricher enricher,
                                            ObjectPredicate objectPredicate) throws AutomationPackageManagerException {
        return createOrUpdateAutomationPackage(false, true, null,
                apSource, keywordLibrarySource,
                apVersion, activationExpr, enricher, objectPredicate, false, actorUser, forceUpload, checkForSameOrigin).getId();
    }

    /**
     * Creates new or updates the existing automation package
     *
     * @param allowUpdate     whether update existing package is allowed
     * @param allowCreate     whether create new package is allowed
     * @param explicitOldId   the explicit package id to be updated (if null, the id will be automatically resolved by package name from packageStream)
     * @param enricher        the enricher used to fill all stored objects (for instance, with product id for multitenant application)
     * @param objectPredicate the filter for automation package
     * @param actorUser
     * @param forceUpload
     * @return the id of created/updated package
     */
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(boolean allowUpdate, boolean allowCreate,
                                                                         ObjectId explicitOldId,
                                                                         AutomationPackageFileSource apSource,
                                                                         AutomationPackageFileSource keywordLibrarySource,
                                                                         String apVersion, String activationExpr,
                                                                         ObjectEnricher enricher, ObjectPredicate objectPredicate,
                                                                         boolean async, String actorUser, boolean forceUpload, boolean checkForSameOrigin) throws AutomationPackageManagerException {
        try {
            try (AutomationPackageArchiveProvider provider = getAutomationPackageArchiveProvider(apSource, objectPredicate)) {
                return createOrUpdateAutomationPackage(allowUpdate, allowCreate, explicitOldId, provider, apVersion, activationExpr, false, enricher, objectPredicate, async, keywordLibrarySource, actorUser, forceUpload, checkForSameOrigin);
            }
        } catch (IOException | AutomationPackageReadingException ex) {
            throw new AutomationPackageManagerException("Automation package cannot be created. Caused by: " + ex.getMessage(), ex);
        }
    }

    /**
     * Creates new or updates the existing automation package
     *
     * @param allowUpdate        whether update existing package is allowed
     * @param allowCreate        whether create new package is allowed
     * @param explicitOldId      the explicit package id to be updated (if null, the id will be automatically resolved by package name from packageStream)
     * @param enricher           the enricher used to fill all stored objects (for instance, with product id for multitenant application)
     * @param objectPredicate    the filter for automation package
     * @param checkForSameOrigin
     * @param forceUpload
     * @param actorUser
     * @return the id of created/updated package
     */
    public AutomationPackageUpdateResult createOrUpdateAutomationPackageFromMaven(MavenArtifactIdentifier mavenArtifactIdentifier,
                                                                                  boolean allowUpdate, boolean allowCreate, ObjectId explicitOldId,
                                                                                  String apVersion, String activationExpr,
                                                                                  AutomationPackageFileSource keywordLibrarySource,
                                                                                  ObjectEnricher enricher, ObjectPredicate objectPredicate,
                                                                                  boolean async, boolean checkForSameOrigin, boolean forceUpload, String actorUser) throws AutomationPackageManagerException {
        try {
            validateMavenConfigAndArtifactClassifier(mavenArtifactIdentifier);
            try (AutomationPackageFromMavenProvider provider = new AutomationPackageFromMavenProvider(mavenConfigProvider.getConfig(objectPredicate), mavenArtifactIdentifier)) {
                // TODO: define keyword library from maven
                return createOrUpdateAutomationPackage(allowUpdate, allowCreate, explicitOldId, provider, apVersion, activationExpr, false, enricher, objectPredicate, async, keywordLibrarySource, actorUser, forceUpload, checkForSameOrigin);
            }
        } catch (IOException ex) {
            throw new AutomationPackageManagerException("Automation package cannot be created. Caused by: " + ex.getMessage(), ex);
        }
    }

    public void updateAutomationPackageMetadata(ObjectId id, String apVersion, String activationExpr, ObjectPredicate objectPredicate) {
        AutomationPackage ap = getAutomationPackageById(id, objectPredicate);
        String newApName;

        String oldApVersion = ap.getVersion();
        String oldApName = ap.getAttribute(AbstractOrganizableObject.NAME);
        int versionBeginIndex = oldApVersion == null ? - 1 : oldApName.lastIndexOf(ap.getVersion());
        if (versionBeginIndex > 0) {
            String oldNameWithoutVersion = oldApName.substring(0, versionBeginIndex - AutomationPackageReader.AP_VERSION_SEPARATOR.length());
            newApName = apVersion == null ? oldNameWithoutVersion : oldNameWithoutVersion + AutomationPackageReader.AP_VERSION_SEPARATOR + apVersion;
        } else {
            newApName = apVersion == null ? oldApName : oldApName + AutomationPackageReader.AP_VERSION_SEPARATOR + apVersion;
        }
        ap.addAttribute(AbstractOrganizableObject.NAME, newApName);
        ap.setActivationExpression(activationExpr == null ? null : new Expression(activationExpr));
        ap.setVersion(apVersion);

        // save metadata
        automationPackageAccessor.save(ap);

        // propagate new activation expression to linked keywords and plans
        List<Function> keywords = getPackageFunctions(id);
        for (Function keyword : keywords) {
            keyword.setActivationExpression(activationExpr == null ? null : new Expression(activationExpr));
            try {
                functionManager.saveFunction(keyword);
            } catch (SetupFunctionException | FunctionTypeException e) {
                throw new AutomationPackageManagerException("Unable to persist a keyword in automation package", e);
            }
        }
        for (Plan plan : getPackagePlans(id)) {
            plan.setActivationExpression(activationExpr == null ? null : new Expression(activationExpr));
            planAccessor.save(plan);
        }
    }

    /**
     * Creates new or updates the existing automation package
     *
     * @param allowUpdate               whether update existing package is allowed
     * @param allowCreate               whether create new package is allowed
     * @param explicitOldId             the explicit package id to be updated (if null, the id will be automatically resolved by package name from packageStream)
     * @param automationPackageProvider the automation package content provider
     * @param enricher                  the enricher used to fill all stored objects (for instance, with product id for multitenant application)
     * @param objectPredicate           the filter for automation package
     * @param keywordLibrarySource
     * @param forceUpload
     * @param checkForSameOrigin
     * @return the id of created/updated package
     * @throws SameAutomationPackageOriginException
     * @throws AutomationPackageManagerException
     */
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(boolean allowUpdate, boolean allowCreate, ObjectId explicitOldId,
                                                                         AutomationPackageArchiveProvider automationPackageProvider, String apVersion, String activationExpr,
                                                                         boolean isLocalPackage, ObjectEnricher enricher, ObjectPredicate objectPredicate, boolean async,
                                                                         AutomationPackageFileSource keywordLibrarySource, String actorUser,
                                                                         boolean forceUpload, boolean checkForSameOrigin) throws AutomationPackageManagerException, SameAutomationPackageOriginException {
        AutomationPackageArchive automationPackageArchive;
        AutomationPackageContent packageContent;

        AutomationPackage newPackage = null;
        try {
            automationPackageArchive = automationPackageProvider.getAutomationPackageArchive();
            packageContent = readAutomationPackage(automationPackageArchive, apVersion, isLocalPackage);
        } catch (AutomationPackageReadingException e) {
            throw new AutomationPackageManagerException("Unable to read automation package. Cause: " + e.getMessage(), e);
        }

        AutomationPackage oldPackage;
        if (explicitOldId != null) {
            oldPackage = getAutomationPackageById(explicitOldId, objectPredicate);

            String newName = packageContent.getName();
            String oldName = oldPackage.getAttribute(AbstractOrganizableObject.NAME);
            if (!Objects.equals(newName, oldName)) {
                // the package with the same name shouldn't exist
                AutomationPackage existingPackageWithSameName = getAutomationPackageByName(newName, objectPredicate);

                if (existingPackageWithSameName != null) {
                    throw new AutomationPackageManagerException("Unable to change the package name to '" + newName
                            + "'. Package with the same name already exists (" + existingPackageWithSameName.getId().toString() + ")");
                }
            }
        } else {
            oldPackage = getAutomationPackageByName(packageContent.getName(), objectPredicate);
        }
        if (!allowUpdate && oldPackage != null) {
            throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' already exists");
        }
        if (!allowCreate && oldPackage == null) {
            throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' doesn't exist");
        }

        // validate if we have the APs with same origin
        ResourceOrigin apOrigin = automationPackageProvider.getOrigin();
        String apOriginString = apOrigin == null ? null : apOrigin.toStringRepresentation();

        SimilarAutomationPackages similarAutomationPackages = findAutomationPackagesWithSameOrigin(objectPredicate, keywordLibrarySource, checkForSameOrigin, apOrigin, oldPackage);

        if (!forceUpload) {
            if (similarAutomationPackages.apWithSameOriginExists() || similarAutomationPackages.apWithSameKeywordLibExists()) {
                throw new SameAutomationPackageOriginException(similarAutomationPackages.getApWithSameOrigin(), similarAutomationPackages.getApWithSameKeywordLib());
            }
        }

        // keep old package id

        newPackage = createNewInstance(
                apOriginString,
                automationPackageArchive.getOriginalFileName(),
                packageContent, apVersion, activationExpr, oldPackage, enricher
        );

        // prepare staging collections
        AutomationPackageStaging staging = createStaging();
        List<ObjectEnricher> enrichers = new ArrayList<>();
        if (enricher != null) {
            enrichers.add(enricher);
        }
        enrichers.add(new AutomationPackageLinkEnricher(newPackage.getId().toString()));

        // we enrich all included entities with automation package id
        ObjectEnricher enricherForIncludedEntities = ObjectEnricherComposer.compose(enrichers);

        // always upload the automation package file as resource
        uploadApResource(automationPackageArchive, newPackage, apOriginString, enricherForIncludedEntities, actorUser);

        // upload keyword package if provided
        String keywordLibraryResourceString = uploadKeywordLibrary(keywordLibrarySource, newPackage, packageContent.getName(), enricherForIncludedEntities, objectPredicate, actorUser, false);

        fillStaging(newPackage, staging, packageContent, oldPackage, enricherForIncludedEntities, automationPackageArchive, activationExpr, keywordLibraryResourceString, actorUser, objectPredicate);

        // persist and activate automation package
        log.debug("Updating automation package, old package is " + ((oldPackage == null) ? "null" : "not null" + ", async: " + async));
        boolean immediateWriteLock = tryObtainImmediateWriteLock(newPackage);
        try {
            if (oldPackage == null || !async || immediateWriteLock) {
                //If not async or if it's a new package, we synchronously wait on a write lock and update
                log.info("Updating the automation package " + newPackage.getId().toString() + " synchronously, any running executions on this package will delay the update.");
                ObjectId result = updateAutomationPackage(oldPackage, newPackage,
                        packageContent, staging, enricherForIncludedEntities,
                        immediateWriteLock, automationPackageArchive, keywordLibraryResourceString, actorUser,
                        similarAutomationPackages.getApWithSameOrigin(), automationPackageProvider, keywordLibrarySource, enricher, objectPredicate);
                return new AutomationPackageUpdateResult(oldPackage == null ? AutomationPackageUpdateStatus.CREATED : AutomationPackageUpdateStatus.UPDATED, result, similarAutomationPackages);
            } else {
                // async update
                log.info("Updating the automation package " + newPackage.getId().toString() + " asynchronously due to running execution(s).");
                newPackage.setStatus(AutomationPackageStatus.DELAYED_UPDATE);
                automationPackageAccessor.save(newPackage);
                AutomationPackage finalNewPackage = newPackage;

                // copy to the final variable to use it in lambda expression
                String finalKeywordLibraryResourceString = keywordLibraryResourceString;
                delayedUpdateExecutor.submit(() -> {
                    try {
                        updateAutomationPackage(
                                oldPackage, finalNewPackage, packageContent, staging, enricherForIncludedEntities,
                                false, automationPackageArchive, finalKeywordLibraryResourceString, actorUser,
                                similarAutomationPackages.getApWithSameOrigin(), automationPackageProvider,
                                keywordLibrarySource, enricher, objectPredicate
                        );
                    } catch (Exception e) {
                        log.error("Exception on delayed AP update", e);
                    }
                });
                return new AutomationPackageUpdateResult(
                        AutomationPackageUpdateStatus.UPDATE_DELAYED,
                        newPackage.getId(),
                        similarAutomationPackages
                );
            }
        } finally {
            if (immediateWriteLock) {
                releaseWriteLock(newPackage);
            }
        }
    }

    private SimilarAutomationPackages findAutomationPackagesWithSameOrigin(ObjectPredicate objectPredicate, AutomationPackageFileSource keywordLibrarySource, boolean checkForSameOrigin, ResourceOrigin apOrigin, AutomationPackage oldPackage) {
        SimilarAutomationPackages similarAutomationPackages = new SimilarAutomationPackages();
        if(checkForSameOrigin) {
            // TODO: apply validation for maven snapshots only?
            if (apOrigin != null && apOrigin.getOriginType() == ResourceOriginType.mvn) {
                similarAutomationPackages.setApWithSameOrigin(
                        automationPackageAccessor.getByOrigin(apOrigin.toStringRepresentation())
                                .stream()
                                .map(AbstractIdentifiableObject::getId)
                                .filter(id -> oldPackage == null || !Objects.equals(oldPackage.getId(), id))
                                .collect(Collectors.toList())
                );
            }
            try (AutomationPackageKeywordLibraryProvider keywordLibraryProvider = getKeywordLibraryProvider(keywordLibrarySource, objectPredicate)) {
                ResourceOrigin keywordLibOrigin = keywordLibraryProvider.getOrigin();
                if (keywordLibOrigin != null && keywordLibOrigin.getOriginType() == ResourceOriginType.mvn) {
                    similarAutomationPackages.setApWithSameKeywordLib(
                            automationPackageAccessor.getByKeywordLibOrigin(keywordLibOrigin.toStringRepresentation())
                                    .stream().map(AbstractIdentifiableObject::getId)
                                    .filter(id -> oldPackage == null &&  !Objects.equals(id, oldPackage.getId()))
                                    .collect(Collectors.toList())
                    );
                }
            } catch (IOException e) {
                throw new AutomationPackageManagerException("Unable to get the keyword source: " + keywordLibrarySource, e);
            }
        }
        return similarAutomationPackages;
    }

    public void reuploadOldAutomationPackages(List<ObjectId> automationPackagesForReupload, AutomationPackageArchiveProvider packageArchiveProvider, AutomationPackageFileSource keywordLibSource, ObjectEnricher objectEnricher, ObjectPredicate objectPredicate, String actorUser) {
        if (automationPackagesForReupload == null) {
            return;
        }
        List<ObjectId> failedAps = new ArrayList<>();
        for (ObjectId objectId : automationPackagesForReupload) {
            try {
                log.info("Updating the AP {}", objectId.toHexString());
                AutomationPackage oldPackage = automationPackageAccessor.get(objectId);
                createOrUpdateAutomationPackage(true, false, objectId, packageArchiveProvider, oldPackage.getVersion(),
                        oldPackage.getActivationExpression() == null ? null : oldPackage.getActivationExpression().getScript(),
                        false, objectEnricher, objectPredicate, false, keywordLibSource, actorUser, true, false);
            } catch (Exception e) {
                log.error("Failed to reupload the automation package {}: {}", objectId.toHexString(), e.getMessage());
                failedAps.add(objectId);
            }
        }
        if (!failedAps.isEmpty()) {
            throw new AutomationPackageManagerException("Unable to reupload the old automation packages: " + failedAps.stream().map(ap -> ap.toHexString()).collect(Collectors.toList()));
        }
    }

    private String uploadApResource(AutomationPackageArchive automationPackageArchive, AutomationPackage newPackage, String apOrigin, ObjectEnricher enricher, String actorUser) {
        File originalFile = automationPackageArchive.getOriginalFile();
        if (originalFile == null) {
            return null;
        }
        try (InputStream is = new FileInputStream(originalFile)) {
            Resource resource = resourceManager.createTrackedResource(
                    ResourceManager.RESOURCE_TYPE_FUNCTIONS, false, is, originalFile.getName(), enricher, null, actorUser, apOrigin
            );
            String resourceString = FileResolver.RESOURCE_PREFIX + resource.getId().toString();
            newPackage.setAutomationPackageResource(resourceString);
            return resourceString;
        } catch (IOException | InvalidResourceFormatException e) {
            throw new RuntimeException("General script function cannot be created", e);
        }
    }

    public String uploadKeywordLibrary(AutomationPackageFileSource keywordLibrarySource, AutomationPackage newPackage, String apName, ObjectEnricher enricher, ObjectPredicate objectPredicate, String actorUser, boolean forIsolatedExecution) {
        // TODO: now we check the MD5 hash to prevent uploading duplicated libraries - further we will need more flexible approach (+strange case - the duplicated resource is persisted)
        String keywordLibraryResourceString = null;
        try (AutomationPackageKeywordLibraryProvider keywordLibraryProvider = getKeywordLibraryProvider(keywordLibrarySource, objectPredicate)) {
            File keywordLibrary = keywordLibraryProvider.getKeywordLibrary();
            Resource keywordLibraryResource = null;
            if (keywordLibrary != null) {
                try (FileInputStream fis = new FileInputStream(keywordLibrary)) {
                    // for isolated execution we always use the isolatedAp resource type to support auto cleanup after execution
                    String resourceType = forIsolatedExecution ? ResourceManager.RESOURCE_TYPE_ISOLATED_AP : keywordLibraryProvider.getResourceType();

                    ResourceOrigin origin = keywordLibraryProvider.getOrigin();
                    List<Resource> oldResources = null;
                    if (origin != null && origin.getOriginType() == ResourceOriginType.mvn) {
                        oldResources = resourceManager.findManyByCriteria(Map.of("origin", origin.toStringRepresentation()));
                    }
                    log.info("The new keyword library ({}) has been uploaded as ({})", keywordLibrarySource, keywordLibraryResource);
                    if (oldResources != null && !oldResources.isEmpty()) {
                        log.info("Existing keyword library {} with resource id {} has been detected and will be reused in AP {}", keywordLibrary.getName(), oldResources.get(0).getId().toHexString(), apName);
                        keywordLibraryResource = oldResources.get(0);
                    } else {
                        keywordLibraryResource = resourceManager.createTrackedResource(resourceType, false, fis, keywordLibrary.getName(), enricher, keywordLibraryProvider.getTrackingValue(), actorUser, origin == null ? null : origin.toStringRepresentation());
                    }
                    keywordLibraryResourceString = FileResolver.RESOURCE_PREFIX + keywordLibraryResource.getId().toString();
                    newPackage.setKeywordLibraryOrigin(keywordLibraryProvider.getOrigin() == null ? null : keywordLibraryProvider.getOrigin().toStringRepresentation());
                    newPackage.setKeywordLibraryResource(keywordLibraryResourceString);
                }
            }
        } catch (IOException | InvalidResourceFormatException | AutomationPackageReadingException e) {
            // all these exceptions are technical, so we log the whole stack trace here, but throw the AutomationPackageManagerException
            // to provide the short error message without technical details to the client
            log.error("Unable to upload the keyword library", e);
            throw new AutomationPackageManagerException("Unable to upload the keyword library: " + keywordLibrarySource, e);
        }
        return keywordLibraryResourceString;
    }

    private AutomationPackageKeywordLibraryProvider getKeywordLibraryProvider(AutomationPackageFileSource keywordLibrarySource, ObjectPredicate predicate) {
        if (keywordLibrarySource != null) {
            if (keywordLibrarySource.useMavenIdentifier()) {
                return new KeywordLibraryFromMavenProvider(mavenConfigProvider.getConfig(predicate), keywordLibrarySource.getMavenArtifactIdentifier());
            } else if (keywordLibrarySource.getInputStream() != null) {
                return new KeywordLibraryFromInputStreamProvider(keywordLibrarySource.getInputStream(), keywordLibrarySource.getFileName());
            }
        }
        return new NoKeywordLibraryProvider();
    }

    private AutomationPackageArchiveProvider getAutomationPackageArchiveProvider(AutomationPackageFileSource apFileSource, ObjectPredicate predicate) throws AutomationPackageReadingException {
        if (apFileSource != null) {
            if (apFileSource.useMavenIdentifier()) {
                return new AutomationPackageFromMavenProvider(mavenConfigProvider.getConfig(predicate), apFileSource.getMavenArtifactIdentifier());
            } else if (apFileSource.getInputStream() != null) {
                return new AutomationPackageFromInputStreamProvider(apFileSource.getInputStream(), apFileSource.getFileName());
            }
        }
        throw new AutomationPackageManagerException("The automation package is not provided");
    }

    /**
     * @return all DB entities linked with the automation package
     */
    public Map<String, List<? extends AbstractOrganizableObject>> getAllEntities(ObjectId automationPackageId) {
        Map<String, List<? extends AbstractOrganizableObject>> result = new HashMap<>();
        List<Plan> packagePlans = getPackagePlans(automationPackageId);
        AutomationPackage automationPackage = automationPackageAccessor.get(automationPackageId);
        result.put(PLANS_ENTITY_NAME, packagePlans);
        List<Function> packageFunctions = getPackageFunctions(automationPackageId);
        result.put(AutomationPackageKeyword.KEYWORDS_ENTITY_NAME, packageFunctions);
        List<String> allHooksNames = automationPackageHookRegistry.getOrderedHookFieldNames();
        for (String hookName : allHooksNames) {
            AutomationPackageHook<?> hook = automationPackageHookRegistry.getHook(hookName);
            result.putAll(hook.getEntitiesForAutomationPackage(
                            automationPackageId,
                            new AutomationPackageContext(automationPackage, operationMode, resourceManager, null, null, null, null, extensions)
                    )
            );
        }
        return result;
    }

    private ObjectId updateAutomationPackage(AutomationPackage oldPackage, AutomationPackage newPackage,
                                             AutomationPackageContent packageContent, AutomationPackageStaging staging, ObjectEnricher enricherForIncludedEntities,
                                             boolean alreadyLocked, AutomationPackageArchive automationPackageArchive, String keywordLibraryResource, String actorUser,
                                             List<ObjectId> additionalPackagesForUpdate, AutomationPackageArchiveProvider apArchiveProvider, AutomationPackageFileSource kwLibSource,
                                             ObjectEnricher baseObjectEnricher, ObjectPredicate objectPredicate) {
        ObjectId mainUpdatedAp = null;
        try {
            //If not already locked (i.e. was not able to acquire an immediate write lock)
            if (!alreadyLocked) {
                log.info("Delaying update of the automation package " + newPackage.getId().toString() + " due to running execution(s) using this package.");
                getWriteLock(newPackage);
                log.info("Executions completed, proceeding with the update of the automation package " + newPackage.getId().toString());
            }
            // delete old package entities
            if (oldPackage != null) {
                deleteAutomationPackageEntities(oldPackage, actorUser);
            }
            // persist all staged entities
            persistStagedEntities(newPackage, staging, enricherForIncludedEntities, automationPackageArchive, packageContent, keywordLibraryResource);
            ObjectId result = automationPackageAccessor.save(newPackage).getId();
            logAfterSave(staging, oldPackage, newPackage);
            mainUpdatedAp = result;
        } finally {
            if (!alreadyLocked) {
                releaseWriteLock(newPackage); //only release if lock was acquired in this method
            }
            //Clear delayed status
            newPackage.setStatus(null);
            automationPackageAccessor.save(newPackage);
        }

        reuploadOldAutomationPackages(additionalPackagesForUpdate, apArchiveProvider, kwLibSource, baseObjectEnricher, objectPredicate, actorUser);
        return mainUpdatedAp;
    }

    protected void getWriteLock(AutomationPackage newPackage) {
        automationPackageLocks.writeLock(newPackage.getId().toHexString());
    }

    protected boolean tryObtainImmediateWriteLock(AutomationPackage newPackage) {
        return automationPackageLocks.tryWriteLock(newPackage.getId().toHexString());
    }


    private void releaseWriteLock(AutomationPackage newPackage) {
        automationPackageLocks.writeUnlock(newPackage.getId().toHexString());
    }

    protected void logAfterSave(AutomationPackageStaging staging, AutomationPackage oldPackage, AutomationPackage newPackage) {
        StringBuilder message = new StringBuilder();
        if (oldPackage != null) {
            message.append(String.format("Automation package has been updated (%s).", newPackage.getAttribute(AbstractOrganizableObject.NAME)));
        } else {
            message.append(String.format("New automation package saved (%s).", newPackage.getAttribute(AbstractOrganizableObject.NAME)));
        }
        message.append(String.format(" Plans: %s.", staging.getPlans().size()));
        message.append(String.format(" Functions: %s.", staging.getFunctions().size()));

        for (String additionalField : staging.getAdditionalFields()) {
            List<?> additionalObjects = staging.getAdditionalObjects(additionalField);
            message.append(String.format(" %s: %s.", additionalField, additionalObjects == null ? 0 : additionalObjects.size()));
        }
        log.info(message.toString());
    }

    protected AutomationPackageStaging createStaging(){
        return new AutomationPackageStaging();
    }

    protected void fillStaging(AutomationPackage newPackage, AutomationPackageStaging staging, AutomationPackageContent packageContent, AutomationPackage oldPackage, ObjectEnricher enricherForIncludedEntities,
                               AutomationPackageArchive automationPackageArchive, String evaluationExpression, String keywordLibraryResourceString, String actorUser, ObjectPredicate objectPredicate) {
        staging.getPlans().addAll(preparePlansStaging(newPackage, packageContent, automationPackageArchive, oldPackage, enricherForIncludedEntities, staging.getResourceManager(), evaluationExpression, keywordLibraryResourceString));
        staging.getFunctions().addAll(prepareFunctionsStaging(newPackage, automationPackageArchive, packageContent, enricherForIncludedEntities, oldPackage, staging.getResourceManager(), evaluationExpression, keywordLibraryResourceString));

        List<HookEntry> hookEntries = new ArrayList<>();
        for (String additionalField : packageContent.getAdditionalFields()) {
            hookEntries.add(new HookEntry(additionalField, packageContent.getAdditionalData(additionalField)));
        }

        List<String> orderedEntryNames = automationPackageHookRegistry.getOrderedHookFieldNames();
        // sort the hook entries according to their "natural" order defined by the registry
        hookEntries.sort(Comparator.comparingInt(he -> orderedEntryNames.indexOf(he.fieldName)));

        for (HookEntry hookEntry : hookEntries) {
            try {
                boolean hooked = automationPackageHookRegistry.onPrepareStaging(
                        hookEntry.fieldName,
                        new AutomationPackageContext(newPackage, operationMode, staging.getResourceManager(), automationPackageArchive, packageContent, keywordLibraryResourceString, enricherForIncludedEntities, extensions),
                        packageContent,
                        hookEntry.values,
                        oldPackage, staging, objectPredicate);

                if (!hooked) {
                    log.warn("Additional field in automation package has been ignored and skipped: " + hookEntry.fieldName);
                }
            } catch (Exception e){
                String fieldNameStr = hookEntry.fieldName == null ? "" : " for '" + hookEntry.fieldName + "'";
                // throw AutomationPackageManagerException to be handled as ControllerException in services
                throw new AutomationPackageManagerException("onPrepareStaging hook invocation failed" + fieldNameStr + " in the automation package '" + packageContent.getName() + "'. " + e.getMessage(), e);
            }
        }
    }

    protected void persistStagedEntities(AutomationPackage newPackage, AutomationPackageStaging staging,
                                         ObjectEnricher objectEnricher,
                                         AutomationPackageArchive automationPackageArchive,
                                         AutomationPackageContent packageContent,
                                         String keywordLibraryResource) {
        List<Resource> stagingResources = staging.getResourceManager().findManyByCriteria(null);
        try {
            for (Resource resource: stagingResources) {
                resourceManager.copyResource(resource, staging.getResourceManager());
            }
        } catch (IOException | InvalidResourceFormatException e) {
            throw new AutomationPackageManagerException("Unable to persist a resource in automation package", e);
        } finally {
            staging.getResourceManager().cleanup();
        }

        try {
            for (Function completeFunction : staging.getFunctions()) {
                functionManager.saveFunction(completeFunction);
            }
        } catch (SetupFunctionException | FunctionTypeException e) {
            throw new AutomationPackageManagerException("Unable to persist a keyword in automation package", e);
        }

        for (Plan plan : staging.getPlans()) {
            planAccessor.save(plan);
        }

        // save task parameters and additional objects via hooks
        List<HookEntry> hookEntries = new ArrayList<>();
        for (String additionalField : staging.getAdditionalFields()) {
            hookEntries.add(new HookEntry(additionalField, staging.getAdditionalObjects(additionalField)));
        }
        for (HookEntry hookEntry : hookEntries) {
            try {
                boolean hooked = automationPackageHookRegistry.onCreate(
                        hookEntry.fieldName, hookEntry.values,
                        new AutomationPackageContext(newPackage, operationMode, resourceManager, automationPackageArchive, packageContent, keywordLibraryResource, objectEnricher, extensions)
                );
                if (!hooked) {
                    log.warn("Additional field in automation package has been ignored and skipped: " + hookEntry.fieldName);
                }
            } catch (Exception e){
                String fieldNameStr = hookEntry.fieldName == null ? "" : " for '" + hookEntry.fieldName + "'";
                // throw AutomationPackageManagerException to be handled as ControllerException in services
                throw new AutomationPackageManagerException("onCreate hook invocation failed" + fieldNameStr + " in the automation package '" + packageContent.getName() + "'. " + e.getMessage(), e);
            }

        }
    }

    public <T extends AbstractOrganizableObject & EnricheableObject> void fillEntities(List<T> entities, List<T> oldEntities, ObjectEnricher enricher) {
        Entity.reuseOldIds(entities, oldEntities);
        entities.forEach(enricher);
    }

    public void runExtensionsBeforeIsolatedExecution(AutomationPackage automationPackage, AbstractStepContext executionContext, Map<String, Object> apManagerExtensions, ImportResult importResult){
        try {
            automationPackageHookRegistry.beforeIsolatedExecution(automationPackage, executionContext, apManagerExtensions, importResult);
        } catch (Exception e){
            // throw AutomationPackageManagerException to be handled as ControllerException in services
            throw new AutomationPackageManagerException("beforeIsolatedExecution hook invocation failed in the automation package '" + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + "'. " + e.getMessage(), e);
        }
    }

    protected List<Plan> preparePlansStaging(AutomationPackage newPackage, AutomationPackageContent packageContent, AutomationPackageArchive automationPackageArchive,
                                             AutomationPackage oldPackage, ObjectEnricher enricher, ResourceManager stagingResourceManager,
                                             String evaluationExpression, String keywordLibraryResourceString) {
        List<Plan> plans = packageContent.getPlans();
        AutomationPackagePlansAttributesApplier specialAttributesApplier = new AutomationPackagePlansAttributesApplier(stagingResourceManager);
        specialAttributesApplier.applySpecialAttributesToPlans(newPackage, plans, automationPackageArchive, packageContent, keywordLibraryResourceString, enricher, extensions, operationMode);

        fillEntities(plans, oldPackage != null ? getPackagePlans(oldPackage.getId()) : new ArrayList<>(), enricher);
        if (evaluationExpression != null && !evaluationExpression.isEmpty()){
            for (Plan plan : plans) {
                plan.setActivationExpression(new Expression(evaluationExpression));
            }
        }
        return plans;
    }

    protected List<Function> prepareFunctionsStaging(AutomationPackage newPackage, AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent, ObjectEnricher enricher,
                                                     AutomationPackage oldPackage, ResourceManager stagingResourceManager, String evaluationExpression, String keywordLibraryResourceString) {
        AutomationPackageContext apContext = new AutomationPackageContext(newPackage, operationMode, stagingResourceManager, automationPackageArchive, packageContent, keywordLibraryResourceString, enricher, extensions);
        List<Function> completeFunctions = packageContent.getKeywords().stream().map(keyword -> keyword.prepareKeyword(apContext)).collect(Collectors.toList());

        // get old functions with same name and reuse their ids
        List<Function> oldFunctions = oldPackage == null ? new ArrayList<>() : getPackageFunctions(oldPackage.getId());
        fillEntities(completeFunctions, oldFunctions, enricher);

        if (evaluationExpression != null && !evaluationExpression.isEmpty()) {
            for (Function completeFunction : completeFunctions) {
                completeFunction.setActivationExpression(new Expression(evaluationExpression));
            }
        }
        return completeFunctions;
    }

    protected AutomationPackage createNewInstance(String origin, String fileName, AutomationPackageContent packageContent, String apVersion, String activationExpr, AutomationPackage oldPackage, ObjectEnricher enricher) {
        AutomationPackage newPackage = new AutomationPackage();

        // keep old id
        if (oldPackage != null) {
            newPackage.setId(oldPackage.getId());
        }
        newPackage.addAttribute(AbstractOrganizableObject.NAME, packageContent.getName());
        newPackage.addAttribute(AbstractOrganizableObject.VERSION, packageContent.getVersion());
        newPackage.setAutomationPackageOrigin(origin);

        newPackage.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_FILE_NAME, fileName);
        if (activationExpr != null && !activationExpr.isEmpty()) {
            newPackage.setActivationExpression(new Expression(activationExpr));
            // TODO: why we only set version when the activation expression is passed?
            newPackage.setVersion(apVersion);
        }
        if (enricher != null) {
            enricher.accept(newPackage);
        }
        return newPackage;
    }

    protected AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive, String apVersion, boolean isLocalPackage) throws AutomationPackageReadingException {
        AutomationPackageContent packageContent;
        packageContent = packageReader.readAutomationPackage(automationPackageArchive, apVersion, isLocalPackage);
        if (packageContent == null) {
            throw new AutomationPackageManagerException("Automation package descriptor is missing, allowed names: " + METADATA_FILES);
        } else if (packageContent.getName() == null || packageContent.getName().isEmpty()) {
            throw new AutomationPackageManagerException("Automation package name is missing");
        }
        return packageContent;
    }

    protected List<Plan> deletePlans(AutomationPackage automationPackage) {
        List<Plan> plans = getPackagePlans(automationPackage.getId());
        plans.forEach(plan -> {
            try {
                planAccessor.remove(plan.getId());
            } catch (Exception e) {
                log.error("Error while deleting plan {} for automation package {}",
                        plan.getId().toString(), automationPackage.getAttribute(AbstractOrganizableObject.NAME), e
                );
            }
        });
        return plans;
    }

    protected List<Function> deleteFunctions(AutomationPackage automationPackage) {
        List<Function> functions = getPackageFunctions(automationPackage.getId());
        functions.forEach(function -> {
            try {
                functionManager.deleteFunction(function.getId().toString());
            } catch (FunctionTypeException e) {
                log.error("Error while deleting function {} for automation package {}",
                        function.getId().toString(), automationPackage.getAttribute(AbstractOrganizableObject.NAME), e
                );
            }
        });
        return functions;
    }

    protected List<Resource> deleteResources(AutomationPackage automationPackage) {
        List<Resource> resources = getPackageResources(automationPackage.getId());
        for (Resource resource : resources) {
            try {
                resourceManager.deleteResource(resource.getId().toString());
            } catch (Exception e) {
                log.error("Error while deleting resource {} for automation package {}",
                        resource.getId().toString(), automationPackage.getAttribute(AbstractOrganizableObject.NAME), e
                );
            }
        }
        return resources;
    }

    protected void deleteAdditionalData(AutomationPackage automationPackage, AutomationPackageContext context) {
        try {
            automationPackageHookRegistry.onAutomationPackageDelete(automationPackage, context, null);
        } catch (Exception e){
            // throw AutomationPackageManagerException to be handled as ControllerException in services
            throw new AutomationPackageManagerException("onAutomationPackageDelete hook invocation failed in the automation package '" + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + "'. " + e.getMessage(), e);
        }
    }

    protected List<Function> getFunctionsByCriteria(Map<String, String> criteria) {
        return functionAccessor.findManyByCriteria(criteria).collect(Collectors.toList());
    }

    public List<Function> getPackageFunctions(ObjectId automationPackageId) {
        return getFunctionsByCriteria(AutomationPackageEntity.getAutomationPackageIdCriteria(automationPackageId));
    }

    protected List<Resource> getResourcesByCriteria(Map<String, String> criteria) {
        return resourceManager.findManyByCriteria(criteria);
    }

    public List<Resource> getPackageResources(ObjectId automationPackageId) {
        return getResourcesByCriteria(AutomationPackageEntity.getAutomationPackageIdCriteria(automationPackageId));
    }

    public List<Plan> getPackagePlans(ObjectId automationPackageId) {
        return planAccessor.findManyByCriteria(AutomationPackageEntity.getAutomationPackageIdCriteria(automationPackageId)).collect(Collectors.toList());
    }

    public AutomationPackageReader getPackageReader() {
        return packageReader;
    }

    public boolean isIsolated() {
        return isIsolated;
    }

    public PlanAccessor getPlanAccessor() {
        return planAccessor;
    }

    public FunctionAccessor getFunctionAccessor() {
        return functionAccessor;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public AutomationPackageAccessor getAutomationPackageAccessor() {
        return automationPackageAccessor;
    }

    public void cleanup() {
        if (isIsolated) {
            this.resourceManager.cleanup();
        } else {
            log.info("Skip automation package cleanup. Cleanup is only supported for isolated (in-memory) automation package manager");
        }
    }

    public String getDescriptorJsonSchema() {
        return packageReader.getDescriptorJsonSchema();
    }

    private static class HookEntry {
        private final String fieldName;
        private final List<?> values;

        public HookEntry(String fieldName, List<?> values) {
            this.fieldName = fieldName;
            this.values = values;
        }
    }

}
