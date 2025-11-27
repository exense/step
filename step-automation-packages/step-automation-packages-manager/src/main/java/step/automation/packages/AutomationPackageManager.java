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

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.InMemoryAutomationPackageAccessorImpl;
import step.automation.packages.library.*;
import step.automation.packages.model.AutomationPackageKeyword;
import step.commons.activation.Expression;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.IndexField;
import step.core.entities.Entity;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.*;
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
import step.plugins.functions.types.CompositeFunction;
import step.repositories.artifact.ResolvedMavenArtifact;
import step.repositories.artifact.SnapshotMetadata;
import step.resources.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static step.automation.packages.AutomationPackageArchive.METADATA_FILES;
import static step.automation.packages.AutomationPackageFileSource.Mode.MANAGED_LIBRARY_NAME;
import static step.automation.packages.AutomationPackageFileSource.Mode.RESOURCE_ID;
import static step.automation.packages.AutomationPackageResourceManager.MANUALLY_CREATED_AP_RESOURCE;
import static step.plans.parser.yaml.YamlPlan.PLANS_ENTITY_NAME;

public class AutomationPackageManager {

    public static final int DEFAULT_READLOCK_TIMEOUT_SECONDS = 60;

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageManager.class);
    private static final String AP_BASE_NAME_ATTR_KEY = "baseName";

    protected final AutomationPackageAccessor automationPackageAccessor;
    protected final FunctionManager functionManager;
    protected final FunctionAccessor functionAccessor;
    private final AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider;
    protected final PlanAccessor planAccessor;
    protected final AutomationPackageReaderRegistry automationPackageReaderRegistry;

    protected final ResourceManager resourceManager;
    protected final AutomationPackageHookRegistry automationPackageHookRegistry;
    private final LinkedAutomationPackagesFinder linkedAutomationPackagesFinder;
    private AutomationPackageResourceManager automationPackageResourceManager;
    protected DefaultProvidersResolver providersResolver;
    protected boolean isIsolated = false;
    protected final AutomationPackageLocks automationPackageLocks;

    private final Map<String, Object> extensions;
    private final ExecutorService delayedUpdateExecutor = Executors.newCachedThreadPool();
    public final AutomationPackageOperationMode operationMode;
    private final int maxParallelVersionsPerPackage;
    private final ObjectHookRegistry objectHookRegistry;


    /**
     * The automation package manager used to store/delete automation packages. To run the automation package in isolated
     * context please use the separate in-memory automation package manager created via
     * {@link AutomationPackageManager#createIsolated(ObjectId, FunctionTypeRegistry, FunctionAccessor)}
     */
    private AutomationPackageManager(AutomationPackageOperationMode operationMode,
                                     AutomationPackageAccessor automationPackageAccessor,
                                     FunctionManager functionManager,
                                     FunctionAccessor functionAccessor,
                                     PlanAccessor planAccessor,
                                     ResourceManager resourceManager,
                                     Map<String, Object> extensions,
                                     AutomationPackageHookRegistry automationPackageHookRegistry,
                                     AutomationPackageReaderRegistry automationPackageReaderRegistry,
                                     AutomationPackageLocks automationPackageLocks,
                                     AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider,
                                     int maxParallelVersionsPerPackage,
                                     ObjectHookRegistry objectHookRegistry) {
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
        this.automationPackageReaderRegistry = automationPackageReaderRegistry;
        this.resourceManager = resourceManager;
        this.automationPackageLocks = automationPackageLocks;
        this.operationMode = Objects.requireNonNull(operationMode);

        this.linkedAutomationPackagesFinder = new LinkedAutomationPackagesFinder(this.resourceManager, this.automationPackageAccessor);
        this.maxParallelVersionsPerPackage = maxParallelVersionsPerPackage;
        this.objectHookRegistry = objectHookRegistry;

        this.providersResolver = new DefaultProvidersResolver(automationPackageReaderRegistry, resourceManager);
        this.automationPackageResourceManager = createAutomationPackageResourceManager();

        addDefaultExtensions();
    }

    private AutomationPackageResourceManager createAutomationPackageResourceManager() {
        return new AutomationPackageResourceManager(
                resourceManager, operationMode, automationPackageAccessor,
                linkedAutomationPackagesFinder, providersResolver.createMavenOperations(mavenConfigProvider)
        );
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
                                                                               AutomationPackageReaderRegistry automationPackageReaderRegistry,
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
                hookRegistry, automationPackageReaderRegistry,
                new AutomationPackageLocks(DEFAULT_READLOCK_TIMEOUT_SECONDS),
                null, -1, null
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
                                                                                  AutomationPackageReaderRegistry automationPackageReaderRegistry,
                                                                                  AutomationPackageHookRegistry hookRegistry,
                                                                                  AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) {

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
                hookRegistry, automationPackageReaderRegistry,
                new AutomationPackageLocks(DEFAULT_READLOCK_TIMEOUT_SECONDS),
                mavenConfigProvider, -1, null
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
                                                                              AutomationPackageReaderRegistry automationPackageReaderRegistry,
                                                                              AutomationPackageLocks locks,
                                                                              AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider,
                                                                              int maxParallelVersionsPerPackage, ObjectHookRegistry objectHookRegistry
    ) {
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
                automationPackageReaderRegistry,
                locks,
                mavenConfigProvider,
                maxParallelVersionsPerPackage,
                objectHookRegistry
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
        return createIsolatedAutomationPackageManager(isolatedContextId, functionTypeRegistry, mainFunctionAccessor, getAutomationPackageReaderRegistry(), automationPackageHookRegistry, mavenConfigProvider);
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

    /**
     * @throws AutomationPackageAccessException if the automation package is not acceptable in current context
     */
    public void removeAutomationPackage(ObjectId id, String actorUser, ObjectPredicate objectPredicate, WriteAccessValidator writeAccessValidator) throws AutomationPackageManagerException {
        AutomationPackage automationPackage = getAutomationPackageById(id, objectPredicate);
        checkAccess(automationPackage, writeAccessValidator);
        String automationPackageId = automationPackage.getId().toHexString();
        if (automationPackageLocks.tryWriteLock(automationPackageId)) {
            try {
                deleteAutomationPackageEntities(automationPackage, null, actorUser, writeAccessValidator);
                automationPackageAccessor.remove(automationPackage.getId());
                log.info("Automation package ({}) has been removed", id);
            } finally {
                automationPackageLocks.releaseAndRemoveLock(automationPackageId);
            }
        } else {
            throw new AutomationPackageManagerException("Automation package cannot be removed while executions using it are running.");
        }
    }

    protected void deleteAutomationPackageEntities(AutomationPackage automationPackage, AutomationPackage newPackage, String actorUser, WriteAccessValidator writeAccessValidator) {
        deleteFunctions(automationPackage);
        deletePlans(automationPackage);

        // remove all internal resources (scripts, files etc.) and the main automation file + automation lib if there are no other APs linked with these files
        deleteResources(automationPackage, newPackage, writeAccessValidator);

        // schedules will be deleted in deleteAdditionalData via hooks
        deleteAdditionalData(automationPackage, new AutomationPackageContext(automationPackage, operationMode, resourceManager,
                null, actorUser, null, extensions));
    }

    /**
     * Creates new or updates the existing automation package
     *
     * @param parameters the operation parameters
     * @return the id of created/updated package
     */
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(AutomationPackageUpdateParameter parameters) throws AutomationPackageManagerException, AutomationPackageAccessException {
        try {
            try (AutomationPackageLibraryProvider apLibProvider = getAutomationPackageLibraryProvider(parameters.apLibrarySource, parameters.objectPredicate);
                 AutomationPackageArchiveProvider provider = getAutomationPackageArchiveProvider(parameters.apSource, parameters.objectPredicate, apLibProvider)) {
                return createOrUpdateAutomationPackage(provider, apLibProvider, parameters);
            }
        } catch (IOException | AutomationPackageReadingException | ManagedLibraryMissingException ex) {
            throw new AutomationPackageManagerException("Automation package cannot be created.", ex, true);
        }
    }

    /**
     * Creates new or updates the existing automation package
     *
     * @param automationPackageProvider  the automation package content provider
     * @param apLibraryProvider the package library provider
     * @param parameters the operation parameters
     * @return the id of created/updated package
     * @throws AutomationPackageManagerException in case of error while creating or updating this package
     */
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(AutomationPackageArchiveProvider automationPackageProvider,
                                                                         AutomationPackageLibraryProvider apLibraryProvider,
                                                                         AutomationPackageUpdateParameter parameters) throws AutomationPackageManagerException {
        AtomicReference<Resource> packageResource = new AtomicReference<>(null);
        AtomicReference<Resource> libraryResource = new AtomicReference<>(null);
        String packageName = "undefined";
        String packageId = "undefined";
        AtomicBoolean isRunningAsync = new AtomicBoolean(false);
        AtomicReference<AutomationPackageStaging> staging = new AtomicReference<>(null);
        try (AutomationPackageArchive automationPackageArchive = automationPackageProvider.getAutomationPackageArchive()) {
            AutomationPackage newPackage;
            AutomationPackageContent packageContent = readAutomationPackage(automationPackageArchive, parameters.versionName, parameters.isLocalPackage);

            AutomationPackage oldPackage = findOldPackage(parameters.explicitOldId, parameters.objectPredicate, packageContent);

            if (!parameters.allowUpdate && oldPackage != null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' already exists");
            }
            if (!parameters.allowCreate && oldPackage == null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' doesn't exist");
            }

            //Implement limit on parallel deployments of multiple version of the same AP (based on its base name, i.e. without AP version appended to it)
            //Limit is only applied for a value greater than 0
            if (maxParallelVersionsPerPackage > 0 && !parameters.reloading) {
                List<AutomationPackage> otherVersions = findOtherVersionsOfPackage(Optional.ofNullable(oldPackage).map(AbstractIdentifiableObject::getId).orElse(null), parameters.objectPredicate, packageContent);
                if (otherVersions.size() >= maxParallelVersionsPerPackage) {
                    throw new AutomationPackageManagerException("The maximum number of parallel versions (" + maxParallelVersionsPerPackage + ") is reached for the package '" + packageContent.getBaseName() + "'. You must clean up older versions of this package before deploying new ones, or contact your administrator to increase this limit.");
                }
            }

            if (oldPackage != null) {
                packageId = oldPackage.getId().toHexString();
                checkAccess(oldPackage, parameters.writeAccessValidator);
            }

            Set<ObjectId> apsWithSamePackage;
            Set<ObjectId> apsWithSameLibrary;
            ConflictingAutomationPackages conflictingAutomationPackages;
            Date beforeResourcesUpdates = new Date();
            if ((automationPackageProvider.isModifiableResource() && automationPackageProvider.hasNewContent()) ||
                    (apLibraryProvider.isModifiableResource() && apLibraryProvider.hasNewContent())) {
                // validate if we have the APs with same origin
                conflictingAutomationPackages = linkedAutomationPackagesFinder.findConflictingPackages(automationPackageProvider,
                        parameters.objectPredicate, apLibraryProvider,
                        parameters.checkForSameOrigin, oldPackage);
                apsWithSamePackage = conflictingAutomationPackages.getApWithSameOrigin();
                apsWithSameLibrary = conflictingAutomationPackages.getApWithSameLibrary();

            } else {
                apsWithSamePackage = Collections.emptySet();
                apsWithSameLibrary = Collections.emptySet();
                conflictingAutomationPackages = new ConflictingAutomationPackages();
            }

            // keep old package id
            newPackage = createNewInstance(
                    automationPackageArchive.getOriginalFileName(),
                    packageContent, oldPackage, parameters
            );
            packageName = newPackage.getAttribute(AbstractOrganizableObject.NAME);
            packageId = newPackage.getId().toHexString();

            // prepare staging collections
            staging.set(createStaging());
            List<ObjectEnricher> enrichers = new ArrayList<>();
            if (parameters.enricher != null) {
                enrichers.add(parameters.enricher);
            }
            enrichers.add(new AutomationPackageLinkEnricher(packageId));

            // we enrich all included entities with automation package id
            ObjectEnricher enricherForIncludedEntities = ObjectEnricherComposer.compose(enrichers);

            //We first make sure we would be able to update or reuse the library before modifying the package itself
            automationPackageResourceManager.validateUploadOrReuseAutomationPackageLibrary(apLibraryProvider, parameters, true);

            // always upload the automation package file as resource
            // NOTE: for the main ap resource don't need to enrich the resource with automation package id (because the same resource can be shared between several automation packages) - so we use simple enricher
            packageResource.set(automationPackageResourceManager.uploadOrReuseApResource(
                    automationPackageProvider, automationPackageArchive, newPackage,
                    parameters, true, apsWithSamePackage.isEmpty() || parameters.forceRefreshOfSnapshots));

            // upload automation package library if provided
            // NOTE: for ap lib we don't need to enrich the resource with automation package id (because the same lib can be shared between several automation packages) - so we use simple enricher
            libraryResource.set(automationPackageResourceManager.uploadOrReuseAutomationPackageLibrary(
                    apLibraryProvider, newPackage, parameters, true,
                    apsWithSameLibrary.isEmpty() || parameters.forceRefreshOfSnapshots));

            fillStaging(newPackage, staging.get(), packageContent, oldPackage, enricherForIncludedEntities, automationPackageArchive, parameters.actorUser, parameters.objectPredicate);

            // persist and activate automation package
            log.debug("Updating automation package, old package is " + ((oldPackage == null) ? "null" : "not null" + ", async: " + parameters.async));

            Set<ObjectId> apForReload = new HashSet<>();
            if (parameters.forceRefreshOfSnapshots && !parameters.reloading) {
                Optional.ofNullable(packageResource.get()).filter(r -> r.getLastModificationDate().getTime() > beforeResourcesUpdates.getTime()).ifPresent(p -> apForReload.addAll(apsWithSamePackage));
                Optional.ofNullable(libraryResource.get()).filter(r -> r.getLastModificationDate().getTime() > beforeResourcesUpdates.getTime()).ifPresent(p -> apForReload.addAll(apsWithSameLibrary));
            }
            boolean immediateWriteLock = tryObtainImmediateWriteLock(newPackage);
            if (oldPackage == null || !parameters.async || immediateWriteLock) {
                //If not async or if it's a new package, we synchronously wait on a write lock and update
                log.info("Updating the automation package '{}':{} synchronously, any running executions on this package will delay the update.", packageName, packageId);
                ObjectId result = performUpdateTasks(parameters, oldPackage, newPackage, packageContent, staging.get(), enricherForIncludedEntities, apForReload, packageResource.get(), libraryResource.get(), isRunningAsync.get(), immediateWriteLock);
                Set<String> warnings = getWarnings(parameters, conflictingAutomationPackages);
                return new AutomationPackageUpdateResult(oldPackage == null ? AutomationPackageUpdateStatus.CREATED : AutomationPackageUpdateStatus.UPDATED, result, conflictingAutomationPackages, warnings);
            } else {
                // async update
                log.info("Updating the automation package '{}':{} asynchronously due to running execution(s).", packageName, packageId);
                // update asynchronously
                isRunningAsync.set(true);
                delayedUpdateExecutor.submit(() -> {
                    performUpdateTasks(parameters, oldPackage, newPackage, packageContent, staging.get(), enricherForIncludedEntities, apForReload, packageResource.get(), libraryResource.get(), isRunningAsync.get(), immediateWriteLock);
                });
                Set<String> warnings = getWarnings(parameters, conflictingAutomationPackages);
                return new AutomationPackageUpdateResult(
                        AutomationPackageUpdateStatus.UPDATE_DELAYED,
                        newPackage.getId(),
                        conflictingAutomationPackages, warnings
                );
            }
        } catch (Throwable e) {
            log.error("Error while updating the automation package with name '{}':{}}.", packageName, packageId);
            handleAutomationPackageDeploymentErrors(parameters, packageName, packageId, packageResource.get(), libraryResource.get());
            throw (e instanceof AutomationPackageManagerException) ? (AutomationPackageManagerException) e: new AutomationPackageManagerException("Unable to update the automation package '" + packageName + "':" + packageId + ".", e, true);
        } finally {
            if (!isRunningAsync.get() && staging.get() != null) {
                staging.get().getResourceManager().cleanup();
            }
        }
    }

    private ObjectId performUpdateTasks(AutomationPackageUpdateParameter parameters, AutomationPackage oldPackage, AutomationPackage newPackage,
                                    AutomationPackageContent packageContent, AutomationPackageStaging staging, ObjectEnricher enricherForIncludedEntities,
                                    Set<ObjectId> apForReload, Resource packageResource, Resource libraryResource,
                                    boolean isRunningAsync, boolean alreadyLocked) {
        try {
            ObjectId automationPackageId;
            try {
                automationPackageId = updateAutomationPackage(
                        oldPackage, newPackage, packageContent, staging, enricherForIncludedEntities,
                        alreadyLocked, parameters
                );
            } finally {
                if (alreadyLocked) {
                    releaseWriteLock(newPackage);
                }
            }
            //Reload linked packaged if not already in a reloading state. Also, cleanup unused and outdated revision of the resources
            if (!parameters.reloading) {
                reloadRelatedAutomationPackages(apForReload, parameters);
                //Cleanup unused revision (for instance if a new snapshot version was downloaded to a resource
                Optional.ofNullable(packageResource).ifPresent(r-> automationPackageResourceManager.deleteUnusedResourceRevisions(r));
                Optional.ofNullable(libraryResource).ifPresent(r-> automationPackageResourceManager.deleteUnusedResourceRevisions(r));
            }
            return automationPackageId;
        } catch (Exception e) {
            //Exception handling is already implemented in the caller when running synchronously, we must implement it here for the asynchronous update
            if (isRunningAsync) {
                String packageName = Objects.requireNonNullElse(newPackage.getAttribute(AbstractOrganizableObject.NAME), "unknown");
                String packageId = newPackage.getId().toHexString();
                log.error("Error while performing the delayed update for the automation package '{}':{}",
                        packageName, packageId, e);
                handleAutomationPackageDeploymentErrors(parameters, packageName, packageId, packageResource, libraryResource);
                throw (e instanceof AutomationPackageManagerException) ? (AutomationPackageManagerException) e :
                        new AutomationPackageManagerException("Unable to update the automation package '" + packageName + "':" + packageId + ".", e, true);
            } else {
                throw e;
            }
        } finally {
            if (isRunningAsync && staging != null) {
                staging.getResourceManager().cleanup();
            }
        }
    }

    /**
     * In case of error while (re)deploying an automation package we must clean up resources that were created but aren't used
     * Since resources can be used by multiple APs we need to first ensure that they are unsued before deleting them
     * @param parameters the deployment parameters
     * @param packageResource the package resource that may need cleanup
     * @param packageName the name of the AP
     * @param libraryResource the library resource that my need cleanup
     */
    private void handleAutomationPackageDeploymentErrors(AutomationPackageUpdateParameter parameters, String packageName, String packageId, Resource packageResource, Resource libraryResource) {
        try {
            //Cleanup the package resource if it isn't used by any automation packages
            if (packageResource != null && linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(packageResource.getId().toHexString(), List.of()).isEmpty()) {
                deleteMainApResource(packageName, parameters.writeAccessValidator, packageResource.getId().toHexString());
            }
            //Cleanup the library resource if it isn't used by any automation packages
            if (libraryResource != null && linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(libraryResource.getId().toHexString(), List.of()).isEmpty()) {
                deleteMainApResource(packageName, parameters.writeAccessValidator, libraryResource.getId().toHexString());
            }
        } catch (Throwable e1) {
            log.error("Resources could not be cleaned up while handling automation package update errors for package '{}':{}.", packageName, packageId, e1);
        }
    }

    private static Set<String> getWarnings(AutomationPackageUpdateParameter parameters, ConflictingAutomationPackages conflictingAutomationPackages) {
        Set<String> warnings = new TreeSet<>();
        if (!parameters.forceRefreshOfSnapshots) {
            if (conflictingAutomationPackages.apWithSameOriginExists()) {
                warnings.add("This automation package is using an outdated SNAPSHOT content. " +
                        "The snapshot could not be updated automatically because it's used by other automation packages. You can either use the UI refresh action or the CLI 'forceRefreshOfSnapshots' option to force its update and reload all related automation packages.");
            }
            if (conflictingAutomationPackages.apWithSameLibraryExists()) {
                warnings.add("This automation package is using a library with an outdated SNAPSHOT content. " +
                        "The snapshot could not be updated automatically because it's used by other automation packages. You can either use the UI refresh action for libraries or the CLI 'forceRefreshOfSnapshots' option to force its update and reload all related automation packages.");
            }
        }
        return warnings;
    }

    private List<AutomationPackage> findOtherVersionsOfPackage(ObjectId currentPackageId, ObjectPredicate objectPredicate, AutomationPackageContent packageContent) {
        Stream<AutomationPackage> stream = StreamSupport.stream(automationPackageAccessor.findManyByAttributes(Map.of(AP_BASE_NAME_ATTR_KEY, packageContent.getBaseName())), false);
        if (objectPredicate != null) {
            stream = stream.filter(objectPredicate);//filter package readable in context
        }
        if (currentPackageId != null) {
            stream = stream.filter(a -> !a.getId().equals(currentPackageId)); //remove current package in case of update
        }
        return stream.collect(Collectors.toList());
    }

    private AutomationPackage findOldPackage(ObjectId explicitOldId, ObjectPredicate objectPredicate, AutomationPackageContent packageContent) {
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
        return oldPackage;
    }


    public void reloadRelatedAutomationPackages(Set<ObjectId> automationPackagesForRedeploy,
                                                AutomationPackageUpdateParameter parameters) throws AutomationPackageRedeployException {
        if (automationPackagesForRedeploy == null || automationPackagesForRedeploy.isEmpty()) {
            return;
        }
        List<ObjectId> failedAps = new ArrayList<>();
        //Set reloading state to all automation packages to be reloaded
        automationPackagesForRedeploy.forEach(id -> updateAutomationPackageStatus(id, AutomationPackageStatus.SCHEDULED_RELOAD));
        for (ObjectId packageId : automationPackagesForRedeploy) {
            AutomationPackage automationPackage = automationPackageAccessor.get(packageId);
            String packageName = automationPackage.getAttribute(AbstractOrganizableObject.NAME);
            try {
                log.info("Reloading the automation package '{}':{}", packageName, packageId);
                if (!FileResolver.isResource(automationPackage.getAutomationPackageResource())) {
                    throw new AutomationPackageManagerException("Automation package '" + packageName + "':" + packageId + " has no linked resource and cannot be reloaded");
                }
                // here we call the `createOrUpdateAutomationPackage` method with parameters specific for reloading (including a flag reloading=true)
                AutomationPackageUpdateParameter redeploymentParameters = new AutomationPackageUpdateParameterBuilder().forRedeployPackage(objectHookRegistry, automationPackage, parameters).build();
                createOrUpdateAutomationPackage(redeploymentParameters);
                updateAutomationPackageStatus(packageId, null);
                log.info("Successfully reloaded the automation package '{}':{}", packageName, packageId);
            } catch (Exception e) {
                updateAutomationPackageStatus(packageId, AutomationPackageStatus.RELOAD_FAILED);
                log.error("Failed to reload the automation package '{}':{}; reason: {}", packageName, packageId, e.getMessage(), e);
                failedAps.add(automationPackage.getId());
            }
        }
        if (!failedAps.isEmpty()) {
            throw new AutomationPackageRedeployException(failedAps);
        }
    }

    /**
     * Only update the status field of the automation package stored in DB
     * @param packageId the object id of the automation package to be updated
     * @param status the new status
     */
    private void updateAutomationPackageStatus(ObjectId packageId, AutomationPackageStatus status) {
        AutomationPackage automationPackage = automationPackageAccessor.get(packageId);
        updateAutomationPackageWithStatus(automationPackage, status);
    }

    /**
     * Set the status and save the provided automation package object to DB
     * @param automationPackage the automation package object to be saved with the provided status
     * @param status the new status
     */
    private void updateAutomationPackageWithStatus(AutomationPackage automationPackage, AutomationPackageStatus status) {
        automationPackage.setStatus(status);
        automationPackageAccessor.save(automationPackage);
    }


    public Resource createAutomationPackageResource(String resourceType, AutomationPackageFileSource fileSource,
                                                    String managedLibraryName, AutomationPackageAccessParameters parameters) throws AutomationPackageManagerException {
        try {
            validateSourceMode(fileSource);
            switch (resourceType) {
                case ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY:
                    //When creating a managed library the name is mandatory
                    if (StringUtils.isBlank(managedLibraryName)) {
                        throw new AutomationPackageManagerException("The name of the managed library is mandatory");
                    }
                    //When creating a managed library the name must be unique
                    Resource resourceByNameAndType = resourceManager.getResourceByNameAndType(managedLibraryName, ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY, parameters.objectPredicate);
                    if (resourceByNameAndType != null) {
                        throw new AutomationPackageManagerException("A managed library with the name '"  + managedLibraryName + "' already exists");
                    }
                    try (AutomationPackageLibraryProvider automationPackageLibraryProvider = getAutomationPackageLibraryProvider(fileSource, parameters.objectPredicate)) {
                        try (AutomationPackageLibraryProvider managedLibraryProvider = new ManagedLibraryProvider(automationPackageLibraryProvider, null, managedLibraryName)) {
                            return automationPackageResourceManager.uploadOrReuseAutomationPackageLibrary(
                                    managedLibraryProvider,
                                    null, parameters, false,
                                    true);
                        }
                    } catch (IOException | ManagedLibraryMissingException e) {
                        throw new AutomationPackageManagerException("Automation package library provider exception.", e, true);
                    }
                case ResourceManager.RESOURCE_TYPE_AP_LIBRARY:
                    // We upload the new resource for package library. Existing resource cannot be reused - to update existing AP resources there is a separate 'refresh' action

                    try (AutomationPackageLibraryProvider automationPackageLibraryProvider = getAutomationPackageLibraryProvider(fileSource, parameters.objectPredicate)) {
                        return automationPackageResourceManager.uploadOrReuseAutomationPackageLibrary(
                                automationPackageLibraryProvider,
                                null, parameters, false,
                                true);
                    } catch (IOException | ManagedLibraryMissingException e) {
                        throw new AutomationPackageManagerException("Automation package library provider exception.", e, true);
                    }
                case ResourceManager.RESOURCE_TYPE_AP:
                    // We upload the new main resource for AP. Existing resource cannot be reused - to update existing AP resources there is a separate 'refresh' action
                    try (AutomationPackageArchiveProvider automationPackageArchiveProvider = getAutomationPackageArchiveProvider(fileSource, parameters.objectPredicate, new NoAutomationPackageLibraryProvider())) {
                        AutomationPackageArchive apArchive = automationPackageArchiveProvider.getAutomationPackageArchive();
                        return automationPackageResourceManager.uploadOrReuseApResource(
                                automationPackageArchiveProvider, apArchive, null,
                                parameters,
                                false,
                                true);
                    } catch (IOException e) {
                        throw new AutomationPackageManagerException("Automation package library provider exception.", e, true);
                    }
                default:
                    throw new AutomationPackageManagerException("Unsupported resource type: " + resourceType);
            }
        } catch (AutomationPackageReadingException ex) {
            throw new AutomationPackageManagerException("Cannot create new resource (" + resourceType + ").", ex, true);
        }
    }

    public Resource updateAutomationPackageManagedLibrary(String id, AutomationPackageFileSource libraryFileSource, String newManagedLibraryName, AutomationPackageUpdateParameter automationPackageUpdateParameter) {
        Resource resource;
        Resource updatedResource;
        try {
            validateSourceMode(libraryFileSource);
            //validate Source type, only RESOURCE_TYPE_AP_MANAGED_LIBRARY are supported for updates
            resource = resourceManager.getResource(id);
            if (resource == null) {
                throw new AutomationPackageManagerException("The managed library with id : " + id + " does not exists.");
            }
            if (! ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY.equals(resource.getResourceType())) {
                throw new AutomationPackageManagerException("Only managed library can be updated, the resource provided has an incorrect type: " + resource.getResourceType());
            }
            if (StringUtils.isBlank(newManagedLibraryName)) {
                throw new AutomationPackageManagerException("The managed library name cannot be null or empty");
            }
            if (!newManagedLibraryName.equals(resource.getAttribute(AbstractOrganizableObject.NAME))) {
                //When changing the name of a managed library the name must still be unique
                Resource resourceByNameAndType = resourceManager.getResourceByNameAndType(newManagedLibraryName, ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY, automationPackageUpdateParameter.objectPredicate);
                if (resourceByNameAndType != null) {
                    throw new AutomationPackageManagerException("Cannot rename the managed library, another managed library with the name '"  + newManagedLibraryName + "' already exists");
                }
            }
            try (AutomationPackageLibraryProvider automationPackageLibraryProvider = getAutomationPackageLibraryProvider(libraryFileSource, automationPackageUpdateParameter.objectPredicate)) {
                try (AutomationPackageLibraryProvider managedLibraryProvider = new ManagedLibraryProvider(automationPackageLibraryProvider, resource, newManagedLibraryName)) {
                    updatedResource = automationPackageResourceManager.uploadOrReuseAutomationPackageLibrary(
                            managedLibraryProvider,
                            null, automationPackageUpdateParameter, true,
                            true);
                }
            } catch (IOException | ManagedLibraryMissingException e) {
                throw new AutomationPackageManagerException("Automation package library provider exception.", e, true);
            }
        } catch (AutomationPackageReadingException ex) {
            throw new AutomationPackageManagerException("Cannot update the managed library with id " + id + ".", ex, true);
        }
        //Now that we updated the managed library, we trigger the reload of APs using it
        Set<ObjectId> automationPackagesIdsByResourceId = linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(resource.getId().toHexString(), List.of());
        reloadRelatedAutomationPackages(automationPackagesIdsByResourceId, automationPackageUpdateParameter);
        //Cleanup unused revision (for instance if a new snapshot version was downloaded to a resource
        Optional.ofNullable(updatedResource).ifPresent(r-> automationPackageResourceManager.deleteUnusedResourceRevisions(r));
        return updatedResource;
    }

    private static void validateSourceMode(AutomationPackageFileSource fileSource) {
        AutomationPackageFileSource.Mode mode = fileSource.getMode();
        if (!mode.equals(AutomationPackageFileSource.Mode.INPUT_STREAM) && !mode.equals(AutomationPackageFileSource.Mode.MAVEN)) {
            throw new AutomationPackageManagerException("The content of a new resource can only be provided as file or maven coordinates");
        }
    }

    public AutomationPackageLibraryProvider getAutomationPackageLibraryProvider(AutomationPackageFileSource apLibrarySource,
                                                                                ObjectPredicate predicate) throws AutomationPackageReadingException, ManagedLibraryMissingException {
        return this.providersResolver.getAutomationPackageLibraryProvider(apLibrarySource, predicate, mavenConfigProvider);
    }

    public AutomationPackageArchiveProvider getAutomationPackageArchiveProvider(AutomationPackageFileSource apFileSource,
                                                                                ObjectPredicate predicate,
                                                                                AutomationPackageLibraryProvider apLibraryProvider) throws AutomationPackageReadingException {
        return this.providersResolver.getAutomationPackageArchiveProvider(apFileSource, predicate, mavenConfigProvider, apLibraryProvider);
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
                            new AutomationPackageContext(automationPackage, operationMode, resourceManager, null, null, null, extensions)
                    )
            );
        }
        return result;
    }

    private ObjectId updateAutomationPackage(AutomationPackage oldPackage, AutomationPackage newPackage,
                                             AutomationPackageContent packageContent, AutomationPackageStaging staging, ObjectEnricher enricherForIncludedEntities,
                                             boolean alreadyLocked,
                                             AutomationPackageUpdateParameter parameters) {
        ObjectId mainUpdatedAp = null;
        try {
            //If not already locked (i.e. was not able to acquire an immediate write lock)
            if (!alreadyLocked) {
                String packageName = Objects.requireNonNullElse(newPackage.getAttribute(AbstractOrganizableObject.NAME), "unknown");
                String packageId = newPackage.getId().toHexString();
                log.info("Delaying update of the automation package '{}':{} due to running execution(s).", packageName, packageId);
                updateAutomationPackageWithStatus(newPackage, AutomationPackageStatus.DELAYED_UPDATE);
                getWriteLock(newPackage);
                log.info("Executions completed, proceeding with the update of the automation package '{}':{}", packageName, packageId);
            }
            // delete old package entities
            if (oldPackage != null) {
                deleteAutomationPackageEntities(oldPackage, newPackage, parameters.actorUser, parameters.writeAccessValidator);
            }
            // persist all staged entities
            persistStagedEntities(newPackage, staging, enricherForIncludedEntities, packageContent, parameters.actorUser);
            ObjectId result = automationPackageAccessor.save(newPackage).getId();
            logAfterSave(staging, oldPackage, newPackage);
            mainUpdatedAp = result;
        } finally {
            if (!alreadyLocked) {
                releaseWriteLock(newPackage); //only release if lock was acquired in this method
            }
            //Clear delayed status
            updateAutomationPackageWithStatus(newPackage, null);
        }
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

    protected void fillStaging(AutomationPackage newPackage, AutomationPackageStaging staging, AutomationPackageContent packageContent,
                               AutomationPackage oldPackage, ObjectEnricher enricherForIncludedEntities,
                               AutomationPackageArchive automationPackageArchive, String actorUser, ObjectPredicate objectPredicate) {
        staging.getPlans().addAll(preparePlansStaging(newPackage, packageContent, automationPackageArchive, oldPackage, enricherForIncludedEntities, staging.getResourceManager(), actorUser));
        staging.getFunctions().addAll(prepareFunctionsStaging(newPackage, automationPackageArchive, packageContent, enricherForIncludedEntities, oldPackage, staging.getResourceManager(), actorUser));

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
                        new StagingAutomationPackageContext(newPackage, operationMode, staging.getResourceManager(), automationPackageArchive, packageContent, actorUser, enricherForIncludedEntities, extensions),
                        packageContent,
                        hookEntry.values,
                        oldPackage, staging, objectPredicate);

                if (!hooked) {
                    log.warn("Additional field in automation package has been ignored and skipped: " + hookEntry.fieldName);
                }
            } catch (Exception e){
                String fieldNameStr = hookEntry.fieldName == null ? "" : " for '" + hookEntry.fieldName + "'";
                // throw AutomationPackageManagerException to be handled as ControllerException in services
                throw new AutomationPackageManagerException("onPrepareStaging hook invocation failed" + fieldNameStr + " in the automation package '" + packageContent.getName() + "'.", e, true);
            }
        }
    }

    protected void persistStagedEntities(AutomationPackage newPackage, AutomationPackageStaging staging,
                                         ObjectEnricher objectEnricher,
                                         AutomationPackageContent packageContent,
                                         String actorUser) {
        List<Resource> stagingResources = staging.getResourceManager().findManyByCriteria(null);
        try {
            for (Resource resource: stagingResources) {
                resourceManager.copyResource(resource, staging.getResourceManager(), actorUser);
            }
        } catch (IOException | InvalidResourceFormatException e) {
            throw new AutomationPackageManagerException("Unable to persist a resource in automation package.", e, true);
        }

        for (Function completeFunction : staging.getFunctions()) {
            functionAccessor.save(completeFunction);
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
                        new AutomationPackageContext(newPackage, operationMode, resourceManager, packageContent, actorUser, objectEnricher, extensions)
                );
                if (!hooked) {
                    log.warn("Additional field in automation package has been ignored and skipped: " + hookEntry.fieldName);
                }
            } catch (Exception e){
                String fieldNameStr = hookEntry.fieldName == null ? "" : " for '" + hookEntry.fieldName + "'";
                // throw AutomationPackageManagerException to be handled as ControllerException in services
                throw new AutomationPackageManagerException("onCreate hook invocation failed" + fieldNameStr + " in the automation package '" + packageContent.getName() + "'.", e, true);
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
            throw new AutomationPackageManagerException("beforeIsolatedExecution hook invocation failed in the automation package '" + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + "'.", e, true);
        }
    }

    protected List<Plan> preparePlansStaging(AutomationPackage newPackage, AutomationPackageContent packageContent, AutomationPackageArchive automationPackageArchive,
                                             AutomationPackage oldPackage, ObjectEnricher enricher, ResourceManager stagingResourceManager,
                                             String actorUser) {
        List<Plan> plans = packageContent.getPlans();
        AutomationPackagePlansAttributesApplier specialAttributesApplier = new AutomationPackagePlansAttributesApplier(stagingResourceManager);
        specialAttributesApplier.applySpecialAttributesToPlans(newPackage, plans, automationPackageArchive, packageContent, actorUser, enricher, extensions, operationMode);

        fillEntities(plans, oldPackage != null ? getPackagePlans(oldPackage.getId()) : new ArrayList<>(), enricher);
        //Only propagate metadata to all the plans if any of the metadata impacting the plans is set
        if (newPackage.getActivationExpression() != null || newPackage.getPlansAttributes() != null) {
            propagatePackageMetadataToPlans(newPackage, plans);
        }
        return plans;
    }

    private static void propagatePackageMetadataToPlans(AutomationPackage newPackage, List<Plan> plans) {
        for (Plan plan : plans) {
            plan.setActivationExpression(newPackage.getActivationExpression());
            if (newPackage.getPlansAttributes() != null) {
                plan.getAttributes().putAll(newPackage.getPlansAttributes());
            }
        }
    }

    protected List<Function> prepareFunctionsStaging(AutomationPackage newPackage, AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent, ObjectEnricher enricher,
                                                     AutomationPackage oldPackage, ResourceManager stagingResourceManager, String actorUser) {
        StagingAutomationPackageContext apContext = new StagingAutomationPackageContext(newPackage, operationMode, stagingResourceManager, automationPackageArchive, packageContent, actorUser, enricher, extensions);
        List<Function> completeFunctions = packageContent.getKeywords().stream().map(keyword -> keyword.prepareKeyword(apContext)).collect(Collectors.toList());

        // get old functions with same name and reuse their ids
        List<Function> oldFunctions = oldPackage == null ? new ArrayList<>() : getPackageFunctions(oldPackage.getId());
        fillEntities(completeFunctions, oldFunctions, enricher);
        //Propagate metadata to all the functions
        propagatePackageMetadataToFunctions(newPackage, completeFunctions);
        return completeFunctions;
    }

    private static void propagatePackageMetadataToFunctions(AutomationPackage newPackage, List<Function> completeFunctions) {
        for (Function completeFunction : completeFunctions) {
            completeFunction.setAutomationPackageFile(newPackage.getAutomationPackageResourceRevision());
            completeFunction.setActivationExpression(newPackage.getActivationExpression());
            if (newPackage.getFunctionsAttributes() != null) {
                completeFunction.getAttributes().putAll(newPackage.getFunctionsAttributes());
            }
            //Execute on controller true has priority whether it is defined at package or keyword level and forced for composite Keywords
            completeFunction.setExecuteLocally(newPackage.getExecuteFunctionsLocally() || completeFunction.isExecuteLocally() ||
                    (completeFunction instanceof CompositeFunction));
            if (newPackage.getTokenSelectionCriteria() != null) {
                //Token selection criteria maps are merged; for keys defined twice, values from the package override the ones from the keyword
                Map<String, String> tokenSelectionCriteriaFromFunction = completeFunction.getTokenSelectionCriteria();
                if (tokenSelectionCriteriaFromFunction == null) {
                    completeFunction.setTokenSelectionCriteria(new HashMap<>(newPackage.getTokenSelectionCriteria()));
                } else {
                    tokenSelectionCriteriaFromFunction.putAll(newPackage.getTokenSelectionCriteria());
                }
            }
        }
    }

    /**
     * Create a new instance of an AutomationPackage, in case an oldPackage is passed we create a copy of it. In the whole
     * stack of deploying the package including hooks we pass both potentially to check what has changed, so for now keeping it
     * this way instead of just updating the "old" package
     * @param fileName the filename of the package set as custom field
     * @param packageContent the package content which has been reqd from the file
     * @param oldPackage the old package in case of update operation
     * @param parameters the provided create or update parameters
     * @return the new instance of the AutomationPackage
     */
    protected AutomationPackage createNewInstance(String fileName, AutomationPackageContent packageContent,
                                                  AutomationPackage oldPackage, AutomationPackageUpdateParameter parameters) {
        AutomationPackage newPackage = new AutomationPackage();

        // keep old id
        if (oldPackage != null) {
            newPackage.setId(oldPackage.getId());
        }
        newPackage.addAttribute(AbstractOrganizableObject.NAME, packageContent.getName());
        newPackage.addAttribute(AP_BASE_NAME_ATTR_KEY, packageContent.getBaseName());
        Date currentTime = new Date();
        if (oldPackage == null) {
            newPackage.setCreationDate(currentTime);
            newPackage.setCreationUser(parameters.actorUser);
        } else {
            newPackage.setCreationDate(oldPackage.getCreationDate());
            newPackage.setCreationUser(oldPackage.getCreationUser());
        }
        newPackage.setLastModificationUser(parameters.actorUser);
        newPackage.setLastModificationDate(currentTime);
        //If either versioName or activation expression is set, then both are mandatory
        if (StringUtils.isNotBlank(parameters.activationExpression) || StringUtils.isNotBlank(parameters.versionName)) {
            if (StringUtils.isBlank(parameters.activationExpression)) {
                throw new AutomationPackageManagerException("Activation expression is mandatory when a version name is provided.");
            } else if (StringUtils.isBlank(parameters.versionName)) {
                throw new AutomationPackageManagerException("The version mane is mandatory when an activation expression is provided.");
            }
        }
        newPackage.setVersionName(parameters.versionName);
        Expression expression = Optional.ofNullable(parameters.activationExpression).map(Expression::new).orElse(null);
        newPackage.setActivationExpression(expression);
        newPackage.setPlansAttributes(parameters.plansAttributes);
        newPackage.setFunctionsAttributes(parameters.functionsAttributes);
        newPackage.setTokenSelectionCriteria(parameters.tokenSelectionCriteria);
        newPackage.setExecuteFunctionsLocally(parameters.executionFunctionsLocally);

        newPackage.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_FILE_NAME, fileName);

        if (parameters.enricher != null) {
            parameters.enricher.accept(newPackage);
        }
        return newPackage;
    }

    protected <A extends AutomationPackageArchive> AutomationPackageContent readAutomationPackage(A automationPackageArchive, String apVersion, boolean isLocalPackage) throws AutomationPackageReadingException {
        AutomationPackageContent packageContent;
        AutomationPackageReader<A> reader = automationPackageReaderRegistry.getReader(automationPackageArchive);
        packageContent = reader.readAutomationPackage(automationPackageArchive, apVersion, isLocalPackage);
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

    /**
     * @param newAutomationPackage new (not persisted yet) automation package
     * @param writeAccessValidator validator used to check write access on the resource to be deleted
     */
    protected void deleteResources(AutomationPackage currentAutomationPackage, AutomationPackage newAutomationPackage, WriteAccessValidator writeAccessValidator) {
        // 1. included resources (files within automation package)
        List<Resource> resources = getPackageResources(currentAutomationPackage.getId());
        for (Resource resource : resources) {
            try {
                // included resources are only used within the automation package (not shared between several packages, so they can be simply deleted)
                log.debug("Remove the resource linked with automation package '{}':{}", currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), resource.getId().toHexString());
                resourceManager.deleteResource(resource.getId().toString());
            } catch (Exception e) {
                log.error("Error while deleting resource {} for automation package {}",
                        resource.getId().toString(), currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), e
                );
            }
        }

        // 2. main resources (AP file and AP lib file)
        deleteMainApResourceIfPossible(currentAutomationPackage, newAutomationPackage, currentAutomationPackage.getAutomationPackageResource(), writeAccessValidator);
        deleteMainApResourceIfPossible(currentAutomationPackage, newAutomationPackage, currentAutomationPackage.getAutomationPackageLibraryResource(), writeAccessValidator);
    }

    private void deleteMainApResourceIfPossible(AutomationPackage currentAutomationPackage, AutomationPackage newAutomationPackage, String apResourceToCheck, WriteAccessValidator writeAccessValidator) {
        try {
            if (FileResolver.isResource(apResourceToCheck)) {
                boolean canBeDeleted = true;
                if (newAutomationPackage != null && (Objects.equals(newAutomationPackage.getAutomationPackageResource(), apResourceToCheck) || Objects.equals(newAutomationPackage.getAutomationPackageLibraryResource(), apResourceToCheck))) {
                    log.info("Resource {} cannot be deleted, because it is reused in new automation package: {}", apResourceToCheck, newAutomationPackage.getAttribute(AbstractOrganizableObject.NAME));
                    canBeDeleted = false;
                }

                if (canBeDeleted) {
                    String resourceId = FileResolver.resolveResourceId(apResourceToCheck);
                    Set<ObjectId> otherApsWithSameResource = linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(resourceId, List.of(currentAutomationPackage.getId()));
                    if (!otherApsWithSameResource.isEmpty()) {
                        log.info("Resource {} cannot be deleted, because it is reused in other automation packages: {}", apResourceToCheck, otherApsWithSameResource);
                        canBeDeleted = false;
                    }

                    if (canBeDeleted) {
                        deleteMainApResource(currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), writeAccessValidator, resourceId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while deleting resource {} for automation package {}",
                    apResourceToCheck, currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), e
            );
        }
    }

    private void deleteMainApResource(String apName, WriteAccessValidator writeAccessValidator, String resourceId) {
        Resource resource = resourceManager.getResource(resourceId);
        if (resource != null && ! (boolean) Objects.requireNonNullElse(resource.getCustomField(MANUALLY_CREATED_AP_RESOURCE), false)) {
            try {
                writeAccessValidator.validate(resource);
                log.debug("Remove the resource linked with the automation package '{}':{}", apName, resourceId);
                resourceManager.deleteResource(resourceId);
            } catch (ObjectAccessException e) {
                log.debug("The resource linked with the automation package '{}':{} is not writable and won't be deleted with the package", apName, resourceId);
            }
        }
    }

    protected void deleteAdditionalData(AutomationPackage automationPackage, AutomationPackageContext context) {
        try {
            automationPackageHookRegistry.onAutomationPackageDelete(automationPackage, context, null);
        } catch (Exception e){
            // throw AutomationPackageManagerException to be handled as ControllerException in services
            throw new AutomationPackageManagerException("onAutomationPackageDelete hook invocation failed in the automation package '" + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + "'.", e, true);
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

    public static void checkAccess(AutomationPackage automationPackage, WriteAccessValidator writeAccessValidator) {
        if (writeAccessValidator != null) {
            try {
                writeAccessValidator.validate(automationPackage);
            } catch (ObjectAccessException e) {
                throw new AutomationPackageAccessException(automationPackage, "You're not allowed to edit this automation package: " +
                        getLogRepresentation(automationPackage), e);
            }
        }
    }

    public static String getLogRepresentation(AutomationPackage p) {
        return "'" + p.getAttribute(AbstractOrganizableObject.NAME) + "'(" + p.getId() + ")";
    }

    public AutomationPackageReaderRegistry getAutomationPackageReaderRegistry() {
        return automationPackageReaderRegistry;
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

    public DefaultProvidersResolver getProvidersResolver() {
        return providersResolver;
    }

    /**
     * This method is package protected and only used for Junit tests
     * @param providersResolver
     */
    protected void setProvidersResolver(DefaultProvidersResolver providersResolver) {
        // providers resolver affects the maven operations for AutomationPackageResourceManager, so it should be re-created
        this.providersResolver = providersResolver;
        this.automationPackageResourceManager = createAutomationPackageResourceManager();
    }

    public AutomationPackageMavenConfig getMavenConfig() {
        return mavenConfigProvider == null ? null : mavenConfigProvider.getConfig();
    }

    public void cleanup() {
        if (isIsolated) {
            this.resourceManager.cleanup();
        } else {
            log.info("Skip automation package cleanup. Cleanup is only supported for isolated (in-memory) automation package manager");
        }
    }

    public String getDescriptorJsonSchema() {
        return automationPackageReaderRegistry.getReaderByType(JavaAutomationPackageArchive.TYPE).getDescriptorJsonSchema();
    }

    private static class HookEntry {
        private final String fieldName;
        private final List<?> values;

        public HookEntry(String fieldName, List<?> values) {
            this.fieldName = fieldName;
            this.values = values;
        }
    }

    public AutomationPackageResourceManager getAutomationPackageResourceManager() {
        return automationPackageResourceManager;
    }

    public interface AutomationPackageProvidersResolver {

        AutomationPackageResourceManager.MavenOperations createMavenOperations(AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider);

        AutomationPackageArchiveProvider getAutomationPackageArchiveProvider(AutomationPackageFileSource apFileSource,
                                                                             ObjectPredicate predicate,
                                                                             AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider,
                                                                             AutomationPackageLibraryProvider apLibraryProvider) throws AutomationPackageReadingException;

        AutomationPackageLibraryProvider getAutomationPackageLibraryProvider(AutomationPackageFileSource apLibrarySource,
                                                                             ObjectPredicate predicate,
                                                                             AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) throws AutomationPackageReadingException, ManagedLibraryMissingException;
    }

    public static class DefaultProvidersResolver implements AutomationPackageProvidersResolver {

        private final AutomationPackageReaderRegistry apReaderRegistry;
        private final ResourceManager resourceManager;

        public DefaultProvidersResolver(AutomationPackageReaderRegistry apReaderRegistry, ResourceManager resourceManager) {
            this.apReaderRegistry = apReaderRegistry;
            this.resourceManager = resourceManager;
        }

        @Override
        public AutomationPackageResourceManager.MavenOperations createMavenOperations(AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) {

            return new AutomationPackageResourceManager.MavenOperations() {
                @Override
                public SnapshotMetadata fetchSnapshotMetadata(MavenArtifactIdentifier mavenArtifactIdentifier, Long existingSnapshotTimestamp) throws AutomationPackageReadingException {
                    if (mavenConfigProvider == null || mavenConfigProvider.getConfig() == null) {
                        throw new AutomationPackageReadingException("Unable to fetch metadata for artifact" + mavenArtifactIdentifier.toShortString() + " from maven. Maven configuration is missing");
                    }
                    return MavenArtifactDownloader.fetchSnapshotMetadata(mavenConfigProvider.getConfig(), mavenArtifactIdentifier, existingSnapshotTimestamp);
                }

                @Override
                public ResolvedMavenArtifact getFile(MavenArtifactIdentifier mavenArtifactIdentifier, Long existingSnapshotTimestamp) throws AutomationPackageReadingException {
                    if (mavenConfigProvider == null || mavenConfigProvider.getConfig() == null) {
                        throw new AutomationPackageReadingException("Unable to download the artifact " + mavenArtifactIdentifier.toShortString() + "  from maven. Maven configuration is missing");
                    }
                    return MavenArtifactDownloader.getFile(mavenConfigProvider.getConfig(), mavenArtifactIdentifier, existingSnapshotTimestamp);
                }
            };
        }

        @Override
        public AutomationPackageArchiveProvider getAutomationPackageArchiveProvider(AutomationPackageFileSource apFileSource,
                                                                                    ObjectPredicate predicate,
                                                                                    AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider,
                                                                                    AutomationPackageLibraryProvider apLibraryProvider) throws AutomationPackageReadingException {
            if (apFileSource != null) {
                if (apFileSource.getMode() == AutomationPackageFileSource.Mode.MAVEN) {
                    return createAutomationPackageFromMavenProvider(apReaderRegistry, apFileSource, predicate, mavenConfigProvider, apLibraryProvider, resourceManager);
                } else if (apFileSource.getMode() == AutomationPackageFileSource.Mode.INPUT_STREAM) {
                    return new AutomationPackageFromInputStreamProvider(apReaderRegistry, apFileSource.getInputStream(), apFileSource.getFileName(), apLibraryProvider);
                } else if (apFileSource.getMode() == RESOURCE_ID) {
                    return new AutomationPackageFromResourceIdProvider(apReaderRegistry, resourceManager, apFileSource.getResourceId(), apLibraryProvider, predicate);
                } else if (apFileSource.getMode() == MANAGED_LIBRARY_NAME) {
                    throw new AutomationPackageManagerException("A managed library cannot be used as automation package");
                }else if (apFileSource.getMode() == AutomationPackageFileSource.Mode.NONE) {
                    // automation package archive is mandatory
                    throw new AutomationPackageManagerException("The automation package is not provided");
                }
            }
            throw new AutomationPackageManagerException("The automation package is not provided");
        }

        protected AutomationPackageFromMavenProvider createAutomationPackageFromMavenProvider(AutomationPackageReaderRegistry apReaderRegistry, AutomationPackageFileSource apFileSource,
                                                                                              ObjectPredicate predicate,
                                                                                              AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider,
                                                                                              AutomationPackageLibraryProvider apLibraryProvider,
                                                                                              ResourceManager resourceManager) throws AutomationPackageReadingException {
            return new AutomationPackageFromMavenProvider(apReaderRegistry, mavenConfigProvider.getConfig(),
                    apFileSource.getMavenArtifactIdentifier(), apLibraryProvider, resourceManager, predicate);
        }

        @Override
        public AutomationPackageLibraryProvider getAutomationPackageLibraryProvider(AutomationPackageFileSource apLibrarySource,
                                                                                    ObjectPredicate predicate,
                                                                                    AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) throws AutomationPackageReadingException, ManagedLibraryMissingException {
            if (apLibrarySource != null) {
                if (apLibrarySource.getMode() == AutomationPackageFileSource.Mode.MAVEN) {
                    return createAutomationPackageLibraryFromMavenProvider(apLibrarySource, predicate, mavenConfigProvider, resourceManager);
                } else if (apLibrarySource.getMode() == AutomationPackageFileSource.Mode.INPUT_STREAM) {
                    return new AutomationPackageLibraryFromInputStreamProvider(apLibrarySource.getInputStream(), apLibrarySource.getFileName());
                } else if(apLibrarySource.getMode() == RESOURCE_ID) {
                    return new AutomationPackageLibraryFromResourceIdProvider(resourceManager, apLibrarySource.getResourceId(), predicate);
                } else if (apLibrarySource.getMode() == MANAGED_LIBRARY_NAME) {
                    return new ManagedLibraryProvider(resourceManager, apLibrarySource.getManagedLibraryName(), predicate);
                } else if(apLibrarySource.getMode() == AutomationPackageFileSource.Mode.NONE){
                    return new NoAutomationPackageLibraryProvider();
                } else {
                    throw new AutomationPackageManagerException("Unsupported mode for automation package library source: " + apLibrarySource.getMode());
                }
            } else {
                return new NoAutomationPackageLibraryProvider();
            }
        }

        protected AutomationPackageLibraryFromMavenProvider createAutomationPackageLibraryFromMavenProvider(AutomationPackageFileSource apLibrarySource, ObjectPredicate predicate, AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider, ResourceManager resourceManager) throws AutomationPackageReadingException {
            return new AutomationPackageLibraryFromMavenProvider(mavenConfigProvider.getConfig(), apLibrarySource.getMavenArtifactIdentifier(), resourceManager, predicate);
        }
    }

}
