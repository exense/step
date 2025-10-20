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
import step.automation.packages.library.*;
import step.automation.packages.model.AutomationPackageKeyword;
import step.commons.activation.Expression;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.IndexField;
import step.core.entities.Entity;
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
import step.plugins.functions.types.CompositeFunction;
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
    protected DefaultProvidersResolver providersResolver;
    protected boolean isIsolated = false;
    protected final AutomationPackageLocks automationPackageLocks;

    private final Map<String, Object> extensions;
    private final ExecutorService delayedUpdateExecutor = Executors.newCachedThreadPool();
    public final AutomationPackageOperationMode operationMode;
    private final int maxParallelVersionsPerPackage;


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
                                     int maxParallelVersionsPerPackage) {
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

        this.providersResolver = new DefaultProvidersResolver(automationPackageReaderRegistry, resourceManager);

        this.linkedAutomationPackagesFinder = new LinkedAutomationPackagesFinder(this.resourceManager, this.automationPackageAccessor);
        this.maxParallelVersionsPerPackage = maxParallelVersionsPerPackage;

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
                null, -1
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
                mavenConfigProvider, -1
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
                                                                              int maxParallelVersionsPerPackage
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
                maxParallelVersionsPerPackage
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
    public void removeAutomationPackage(ObjectId id, String actorUser, ObjectPredicate objectPredicate, ObjectPredicate writeAccessPredicate) throws AutomationPackageManagerException {
        AutomationPackage automationPackage = getAutomationPackageById(id, objectPredicate);
        checkAccess(automationPackage, writeAccessPredicate);
        String automationPackageId = automationPackage.getId().toHexString();
        if (automationPackageLocks.tryWriteLock(automationPackageId)) {
            try {
                deleteAutomationPackageEntities(automationPackage, null, actorUser, writeAccessPredicate);
                automationPackageAccessor.remove(automationPackage.getId());
                log.info("Automation package ({}) has been removed", id);
            } finally {
                automationPackageLocks.releaseAndRemoveLock(automationPackageId);
            }
        } else {
            throw new AutomationPackageManagerException("Automation package cannot be removed while executions using it are running.");
        }
    }

    protected void deleteAutomationPackageEntities(AutomationPackage automationPackage, AutomationPackage newPackage, String actorUser, ObjectPredicate writeAccessPredicate) {
        deleteFunctions(automationPackage);
        deletePlans(automationPackage);
        // schedules will be deleted in deleteAdditionalData via hooks
        deleteResources(automationPackage, newPackage, writeAccessPredicate);
        deleteAdditionalData(automationPackage, new AutomationPackageContext(automationPackage, operationMode, resourceManager,
                null,  null, actorUser, null, extensions));
    }

    private Expression getActivationExpression(String apVersion, String activationExpression, String baseApName) {
        if (activationExpression == null || activationExpression.isEmpty()) {
            if (apVersion == null) {
                return null;
            } else {
                //getProperty is used to support binding keys with hyphens and dots
                return new Expression("getProperty('" + baseApName + ".version') == \"" + apVersion + "\"");
            }
        } else {
            return new Expression(activationExpression);
        }
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
        } catch (IOException | AutomationPackageReadingException ex) {
            throw new AutomationPackageManagerException("Automation package cannot be created. Caused by: " + ex.getMessage(), ex);
        }
    }

    /**
     * Creates new or updates the existing automation package
     *
     * @param automationPackageProvider  the automation package content provider
     * @param apLibraryProvider the package library provider
     * @param parameters the operation parameters
     * @return the id of created/updated package
     * @throws AutomationPackageCollisionException in case other packages use the same resource and would be updated by this operation
     * @throws AutomationPackageManagerException in case of error while creating or updating this package
     */
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(AutomationPackageArchiveProvider automationPackageProvider,
                                                                         AutomationPackageLibraryProvider apLibraryProvider,
                                                                         AutomationPackageUpdateParameter parameters) throws AutomationPackageManagerException, AutomationPackageCollisionException, AutomationPackageAccessException {

        try (AutomationPackageArchive automationPackageArchive = automationPackageProvider.getAutomationPackageArchive()) {
            AutomationPackage newPackage;
            AutomationPackageContent packageContent = readAutomationPackage(automationPackageArchive, parameters.automationPackageVersion, parameters.isLocalPackage);

            AutomationPackage oldPackage = findOldPackage(parameters.explicitOldId, parameters.objectPredicate, packageContent);

            if (!parameters.allowUpdate && oldPackage != null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' already exists");
            }
            if (!parameters.allowCreate && oldPackage == null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' doesn't exist");
            }

            //Implement limit on parallel deployments of multiple version of the same AP (based on its base name, i.e. without AP version appened to it)
            //Limit is only applied for a value greater than 0
            if (maxParallelVersionsPerPackage > 0) {
                List<AutomationPackage> otherVersions = findOtherVersionsOfPackage(parameters.explicitOldId, parameters.objectPredicate, packageContent);
                if (otherVersions.size() >= maxParallelVersionsPerPackage) {
                    throw new AutomationPackageManagerException("The maximum number of parallel versions (" + maxParallelVersionsPerPackage + ") is reached for the package '" + packageContent.getBaseName() + "'. You must clean up older versions of this package before deploying new ones, or contact your administrator to increase this limit.");
                }
            }

            if (oldPackage != null) {
                checkAccess(oldPackage, parameters.writeAccessPredicate);
            }

            List<ObjectId> apsForReupload;
            ConflictingAutomationPackages conflictingAutomationPackages;
            if ((automationPackageProvider.isModifiableResource() && automationPackageProvider.hasNewContent()) ||
                    (apLibraryProvider.isModifiableResource() && apLibraryProvider.hasNewContent())) {
                // validate if we have the APs with same origin
                conflictingAutomationPackages = linkedAutomationPackagesFinder.findConflictingPackagesAndCheckAccess(automationPackageProvider,
                        parameters.objectPredicate, parameters.writeAccessPredicate, apLibraryProvider, parameters.allowUpdateOfOtherPackages,
                        parameters.checkForSameOrigin, oldPackage, this);
                apsForReupload = conflictingAutomationPackages.getApWithSameOrigin();
            } else {
                apsForReupload = Collections.emptyList();
                conflictingAutomationPackages = new ConflictingAutomationPackages();
            }

            ResourceOrigin apOrigin = automationPackageProvider.getOrigin();
            // keep old package id
            newPackage = createNewInstance(
                    automationPackageArchive.getOriginalFileName(),
                    packageContent, oldPackage, parameters
            );

            // prepare staging collections
            AutomationPackageStaging staging = createStaging();
            List<ObjectEnricher> enrichers = new ArrayList<>();
            if (parameters.enricher != null) {
                enrichers.add(parameters.enricher);
            }
            enrichers.add(new AutomationPackageLinkEnricher(newPackage.getId().toString()));

            // we enrich all included entities with automation package id
            ObjectEnricher enricherForIncludedEntities = ObjectEnricherComposer.compose(enrichers);


            // TODO: potential issue - in code below we use the accessChecker in uploadApResourceIfRequired and uploadKeywordLibrary and if the check blocks the uploadKeywordLibrary the uploaded ap resource will not be cleaned up
            // always upload the automation package file as resource
            // NOTE: for the main ap resource don't need to enrich the resource with automation package id (because the same resource can be shared between several automation packages) - so we use simple enricher
            uploadApResourceIfRequired(automationPackageProvider, automationPackageArchive, newPackage, apOrigin, parameters);

            // upload automation package library if provided
            // NOTE: for ap lib we don't need to enrich the resource with automation package id (because the same lib can be shared between several automation packages) - so we use simple enricher
            String apLibraryResourceString = uploadAutomationPackageLibrary(apLibraryProvider, newPackage, packageContent.getName(), parameters);

            fillStaging(newPackage, staging, packageContent, oldPackage, enricherForIncludedEntities, automationPackageArchive, apLibraryResourceString, parameters.actorUser, parameters.objectPredicate);

            // persist and activate automation package
            log.debug("Updating automation package, old package is " + ((oldPackage == null) ? "null" : "not null" + ", async: " + parameters.async));
            boolean immediateWriteLock = tryObtainImmediateWriteLock(newPackage);
            try {
                if (oldPackage == null || !parameters.async || immediateWriteLock) {
                    //If not async or if it's a new package, we synchronously wait on a write lock and update
                    log.info("Updating the automation package " + newPackage.getId().toString() + " synchronously, any running executions on this package will delay the update.");
                    ObjectId result = updateAutomationPackage(oldPackage, newPackage,
                            packageContent, staging, enricherForIncludedEntities,
                            immediateWriteLock, automationPackageArchive, apLibraryResourceString,
                            apsForReupload, automationPackageProvider, apLibraryProvider, parameters);
                    return new AutomationPackageUpdateResult(oldPackage == null ? AutomationPackageUpdateStatus.CREATED : AutomationPackageUpdateStatus.UPDATED, result, conflictingAutomationPackages);
                } else {
                    // async update
                    log.info("Updating the automation package " + newPackage.getId().toString() + " asynchronously due to running execution(s).");
                    newPackage.setStatus(AutomationPackageStatus.DELAYED_UPDATE);
                    automationPackageAccessor.save(newPackage);
                    // update asynchronously
                    delayedUpdateExecutor.submit(() -> {
                        try {
                            updateAutomationPackage(
                                    oldPackage, newPackage, packageContent, staging, enricherForIncludedEntities,
                                    false, automationPackageArchive, apLibraryResourceString,
                                    apsForReupload, automationPackageProvider,
                                    apLibraryProvider, parameters
                            );
                        } catch (Exception e) {
                            log.error("Exception on delayed AP update", e);
                        }
                    });
                    return new AutomationPackageUpdateResult(
                            AutomationPackageUpdateStatus.UPDATE_DELAYED,
                            newPackage.getId(),
                            conflictingAutomationPackages
                    );
                }
            } finally {
                if (immediateWriteLock) {
                    releaseWriteLock(newPackage);
                }
            }
        } catch(AutomationPackageReadingException | IOException e){
            throw new AutomationPackageManagerException("Unable to read automation package. Cause: " + e.getMessage(), e);
        }
    }

    private List<AutomationPackage> findOtherVersionsOfPackage(ObjectId explicitOldId, ObjectPredicate objectPredicate, AutomationPackageContent packageContent) {
        Stream<AutomationPackage> stream = StreamSupport.stream(automationPackageAccessor.findManyByAttributes(Map.of(AP_BASE_NAME_ATTR_KEY, packageContent.getBaseName())), false);
        if (objectPredicate != null) {
            stream = stream.filter(objectPredicate);//filter package readable in context
        }
        if (explicitOldId != null) {
            stream = stream.filter(a -> !a.getId().equals(explicitOldId)); //remove current package in case of update
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


    public void updateRelatedAutomationPackages(List<ObjectId> automationPackagesForRedeploy,
                                                AutomationPackageArchiveProvider packageArchiveProvider,
                                                AutomationPackageUpdateParameter parameters) {
        if (automationPackagesForRedeploy == null || automationPackagesForRedeploy.isEmpty() || !packageArchiveProvider.hasNewContent()) {
            return;
        }
        List<ObjectId> failedAps = new ArrayList<>();
        for (ObjectId objectId : automationPackagesForRedeploy) {
            try {
                log.info("Redeploying the AP {}", objectId.toHexString());
                AutomationPackage oldPackage = automationPackageAccessor.get(objectId);

                // here we call the `createOrUpdateAutomationPackage` method with checkForSameOrigin=false to avoid infinite recursive loop
                // we should only redeploy AP using the modified package keeping all other existing attributes
                AutomationPackageUpdateParameter redeploymentParameters = new AutomationPackageUpdateParameterBuilder().forRedeployPackage(oldPackage, parameters).build();
                createOrUpdateAutomationPackage(redeploymentParameters);
            } catch (Exception e) {
                log.error("Failed to redeploy the automation package {}: {}", objectId.toHexString(), e.getMessage());
                failedAps.add(objectId);
            }
        }
        if (!failedAps.isEmpty()) {
            throw new AutomationPackageManagerException("Unable to reupload the old automation packages: " + failedAps.stream().map(ObjectId::toHexString).collect(Collectors.toList()));
        }
    }

    private void uploadApResourceIfRequired(AutomationPackageArchiveProvider apProvider,
                                            AutomationPackageArchive automationPackageArchive,
                                            AutomationPackage newPackage,
                                            ResourceOrigin apOrigin,
                                            AutomationPackageUpdateParameter parameters) {
        File originalFile = automationPackageArchive.getOriginalFile();
        if (originalFile == null) {
            return;
        }

        Resource resource = null;

        List<Resource> existingResource = null;
        if (apProvider.canLookupResources()) {
            existingResource = apProvider.lookupExistingResources(resourceManager, parameters.objectPredicate);
        }

        if (existingResource != null && !existingResource.isEmpty()) {
            resource = existingResource.get(0);

            // we just reuse the existing resource of unmodifiable origin (i.e non-SNAPSHOT)
            // and for SNAPSHOT we keep the same resource id, but update the content if a new version was found
            if (apProvider.isModifiableResource() && apProvider.hasNewContent()) {
                try (FileInputStream is = new FileInputStream(originalFile)) {
                    resource = updateExistingResourceContentAndPropagate(
                            originalFile.getName(),
                            newPackage.getAttribute(AbstractOrganizableObject.NAME), newPackage.getId(),
                            is, apProvider.getSnapshotTimestamp(), resource,
                            parameters.actorUser, parameters.writeAccessPredicate
                    );
                } catch (IOException | InvalidResourceFormatException e) {
                    throw new RuntimeException("Unable to create the resource for automation package", e);
                }
            }
        }

        // create the new resource if the old one cannot be reused
        if (resource == null) {
            try (InputStream is = new FileInputStream(originalFile)) {
                resource = resourceManager.createTrackedResource(
                        ResourceManager.RESOURCE_TYPE_AP, false, is, originalFile.getName(), parameters.enricher, null, parameters.actorUser,
                        apOrigin == null ? null : apOrigin.toStringRepresentation(), apProvider.getSnapshotTimestamp()
                );
            } catch (IOException | InvalidResourceFormatException e) {
                throw new RuntimeException("Unable to create the resource for automation package", e);
            }
        }

        String resourceString = FileResolver.RESOURCE_PREFIX + resource.getId().toString();
        log.info("The resource has been been linked with AP '{}': {}", newPackage.getAttribute(AbstractOrganizableObject.NAME), resourceString);
        newPackage.setAutomationPackageResource(resourceString);
    }

    protected String uploadAutomationPackageLibrary(AutomationPackageLibraryProvider apLibProvider, AutomationPackage newPackage,
                                                    String apName, AutomationPackageUpdateParameter parameters) {
        String apLibraryResourceString = null;
        try {
            File apLibrary = apLibProvider.getAutomationPackageLibrary();
            Resource uploadedResource = null;
            if (apLibrary != null) {
                try (FileInputStream fis = new FileInputStream(apLibrary)) {
                    // for isolated execution we always use the isolatedAp resource type to support auto cleanup after execution
                    String resourceType = this.operationMode == AutomationPackageOperationMode.ISOLATED ? ResourceManager.RESOURCE_TYPE_ISOLATED_AP_LIB : apLibProvider.getResourceType();

                    // we can reuse the existing old resource in case it is identifiable (can be found by origin) and unmodifiable
                    List<Resource> oldResources = null;
                    if (apLibProvider.canLookupResources()) {
                        oldResources = apLibProvider.lookupExistingResources(resourceManager, parameters.objectPredicate);
                    }

                    if (oldResources != null && !oldResources.isEmpty()) {
                        Resource oldResource = oldResources.get(0);
                        if (!apLibProvider.isModifiableResource()) {
                            // for unmodifiable origins we just reused the previously uploaded resource
                            log.info("Existing automation package library {} with resource id {} has been detected and will be reused in AP {}", apLibrary.getName(), oldResource.getId().toHexString(), apName);
                            uploadedResource = oldResource;
                        } else if (apLibProvider.hasNewContent()){
                            // for modifiable resources (i.e. SNAPSHOTS) we can reuse the old resource id and metadata, but we need to update the content if a new version was downloaded
                            uploadedResource = updateExistingResourceContentAndPropagate(apLibrary.getName(),
                                    apName, newPackage.getId(),
                                    fis, apLibProvider.getSnapshotTimestamp(), oldResource,
                                    parameters.actorUser, parameters.writeAccessPredicate
                            );
                        } else {
                            uploadedResource = oldResource;
                        }
                    } else {
                        ResourceOrigin origin = apLibProvider.getOrigin();

                        // old resource is not found - we create a new one
                        uploadedResource = resourceManager.createTrackedResource(
                                resourceType, false, fis, apLibrary.getName(), parameters.enricher, null,
                                parameters.actorUser, origin == null ? null : origin.toStringRepresentation(),
                                apLibProvider.getSnapshotTimestamp()
                        );
                        log.info("The new automation package library ({}) has been uploaded as ({})", apLibProvider, uploadedResource.getId().toHexString());
                    }
                    apLibraryResourceString = FileResolver.RESOURCE_PREFIX + uploadedResource.getId().toString();
                    newPackage.setAutomationPackageLibraryResource(apLibraryResourceString);
                }
            }
        } catch (IOException | InvalidResourceFormatException | AutomationPackageReadingException e) {
            // all these exceptions are technical, so we log the whole stack trace here, but throw the AutomationPackageManagerException
            // to provide the short error message without technical details to the client
            log.error("Unable to upload the automation package library", e);
            throw new AutomationPackageManagerException("Unable to upload the automation package library: " + apLibProvider, e);
        }
        return apLibraryResourceString;
    }

    /**
     * This method is called for modifiable resource (currently only maven snapshot artefact) to update the existing Step
     * resource with the new content and propagate the update to Automation Packages using this resource
     *
     * @param resourceFileName the resource file name
     * @param apName the name of the automation pacakge
     * @param currentApId the automation package Id
     * @param fis the resource input stream
     * @param newOriginTimestamp the artefact snapshot timestamp (null if no new snapshot was downloaded
     * @param oldResource the resource to be updated if required
     * @param actorUser the user triggering this update
     * @param writeAccessPredicate predicate to check if this resource can be updated in this context
     * @return the updated resource
     * @throws IOException in case of error when writing the resource content to the file system
     * @throws InvalidResourceFormatException in case the new resource content is invalid
     * @throws AutomationPackageAccessException in case the resource of linked AP cannot be updated in the current context
     */
    private Resource updateExistingResourceContentAndPropagate(String resourceFileName,
                                                               String apName, ObjectId currentApId,
                                                               FileInputStream fis, Long newOriginTimestamp,
                                                               Resource oldResource, String actorUser,
                                                               ObjectPredicate writeAccessPredicate) throws IOException, InvalidResourceFormatException, AutomationPackageAccessException {
        String resourceId = oldResource.getId().toHexString();
        //Check write access to the resource itself
        if (!writeAccessPredicate.test(oldResource)) {
            String errorMessage = "The existing resource " + resourceId + " for file " + resourceFileName + " referenced by the provided package cannot be modified in the current context.";
            log.error(errorMessage);
            throw new AutomationPackageAccessException(errorMessage);
        }
        // Check write access to other APs using this resource. We cannot reupload the resources if they are linked with another package, which is not accessible
        for (ObjectId apId : linkedAutomationPackagesFinder.findAutomationPackagesByResourceId(resourceId, List.of(currentApId))) {
            checkAccess(automationPackageAccessor.get(apId), writeAccessPredicate);
        }

        log.info("Existing resource {} for file {} will be actualized and reused in AP {}", resourceId, resourceFileName, apName);
        Resource uploadedResource = resourceManager.saveResourceContent(resourceId, fis, resourceFileName, actorUser);
        uploadedResource.setOriginTimestamp(newOriginTimestamp);
        resourceManager.saveResource(uploadedResource);
        return uploadedResource;
    }

    public AutomationPackageLibraryProvider getAutomationPackageLibraryProvider(AutomationPackageFileSource apLibrarySource,
                                                                                ObjectPredicate predicate) throws AutomationPackageReadingException {
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
                            new AutomationPackageContext(automationPackage, operationMode, resourceManager, null, null, null, null, extensions)
                    )
            );
        }
        return result;
    }

    private ObjectId updateAutomationPackage(AutomationPackage oldPackage, AutomationPackage newPackage,
                                             AutomationPackageContent packageContent, AutomationPackageStaging staging, ObjectEnricher enricherForIncludedEntities,
                                             boolean alreadyLocked, AutomationPackageArchive automationPackageArchive,
                                             String apLibraryResource,
                                             List<ObjectId> additionalPackagesForRedeploy,
                                             AutomationPackageArchiveProvider apArchiveProvider, AutomationPackageLibraryProvider apLibProvider,
                                             AutomationPackageUpdateParameter parameters) {
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
                deleteAutomationPackageEntities(oldPackage, newPackage, parameters.actorUser, parameters.writeAccessPredicate);
            }
            // persist all staged entities
            persistStagedEntities(newPackage, staging, enricherForIncludedEntities, automationPackageArchive, packageContent, apLibraryResource, parameters.actorUser);
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

        //Only redeploy APs using the same package if a new snapshot versions was downloaded
        updateRelatedAutomationPackages(additionalPackagesForRedeploy, apArchiveProvider, parameters);
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
                               AutomationPackageArchive automationPackageArchive, String apLibraryResourceString, String actorUser, ObjectPredicate objectPredicate) {
        staging.getPlans().addAll(preparePlansStaging(newPackage, packageContent, automationPackageArchive, oldPackage, enricherForIncludedEntities, staging.getResourceManager(), apLibraryResourceString));
        staging.getFunctions().addAll(prepareFunctionsStaging(newPackage, automationPackageArchive, packageContent, enricherForIncludedEntities, oldPackage, staging.getResourceManager(), apLibraryResourceString));

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
                        new AutomationPackageContext(newPackage, operationMode, staging.getResourceManager(), automationPackageArchive, packageContent, apLibraryResourceString, enricherForIncludedEntities, extensions),
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
                                         String apLibraryResource, String actorUser) {
        List<Resource> stagingResources = staging.getResourceManager().findManyByCriteria(null);
        try {
            for (Resource resource: stagingResources) {
                resourceManager.copyResource(resource, staging.getResourceManager(), actorUser);
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
                        new AutomationPackageContext(newPackage, operationMode, resourceManager, automationPackageArchive, packageContent, apLibraryResource, objectEnricher, extensions)
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
                                             String apLibraryResourceString) {
        List<Plan> plans = packageContent.getPlans();
        AutomationPackagePlansAttributesApplier specialAttributesApplier = new AutomationPackagePlansAttributesApplier(stagingResourceManager);
        specialAttributesApplier.applySpecialAttributesToPlans(newPackage, plans, automationPackageArchive, packageContent, apLibraryResourceString, enricher, extensions, operationMode);

        fillEntities(plans, oldPackage != null ? getPackagePlans(oldPackage.getId()) : new ArrayList<>(), enricher);
        //Only browser the plans if any of the metadata is not null
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
                                                     AutomationPackage oldPackage, ResourceManager stagingResourceManager, String apLibraryResourceString) {
        AutomationPackageContext apContext = new AutomationPackageContext(newPackage, operationMode, stagingResourceManager, automationPackageArchive, packageContent, apLibraryResourceString, enricher, extensions);
        List<Function> completeFunctions = packageContent.getKeywords().stream().map(keyword -> keyword.prepareKeyword(apContext)).collect(Collectors.toList());

        // get old functions with same name and reuse their ids
        List<Function> oldFunctions = oldPackage == null ? new ArrayList<>() : getPackageFunctions(oldPackage.getId());
        fillEntities(completeFunctions, oldFunctions, enricher);
        //Only browser the plans if any of the metadata is not null
        if (newPackage.getActivationExpression() != null || newPackage.getFunctionsAttributes() != null || newPackage.getTokenSelectionCriteria() != null || newPackage.getExecuteFunctionLocally()) {
            propagatePackageMetadataToFunctions(newPackage, completeFunctions);
        }
        return completeFunctions;
    }

    private static void propagatePackageMetadataToFunctions(AutomationPackage newPackage, List<Function> completeFunctions) {
        for (Function completeFunction : completeFunctions) {
            completeFunction.setActivationExpression(newPackage.getActivationExpression());
            if (newPackage.getFunctionsAttributes() != null) {
                completeFunction.getAttributes().putAll(newPackage.getFunctionsAttributes());
            }
            //Execute on controller true has priority whether it is defined at package or keyword level and forced for composite Keywords
            completeFunction.setExecuteLocally(newPackage.getExecuteFunctionLocally() || completeFunction.isExecuteLocally() ||
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
        newPackage.setVersion(parameters.automationPackageVersion);
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
        Expression resolvedActivationExpression = getActivationExpression(parameters.automationPackageVersion, parameters.activationExpression, packageContent.getBaseName());
        newPackage.setActivationExpression(resolvedActivationExpression);
        newPackage.setPlansAttributes(parameters.plansAttributes);
        newPackage.setFunctionsAttributes(parameters.functionsAttributes);
        newPackage.setTokenSelectionCriteria(parameters.tokenSelectionCriteria);
        newPackage.setExecuteFunctionLocally(parameters.executionFunctionsLocally);

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
     * @param writeAccessPredicate predicate use to check write access on the resource to be deleted
     */
    protected void deleteResources(AutomationPackage currentAutomationPackage, AutomationPackage newAutomationPackage, ObjectPredicate writeAccessPredicate) {
        // 1. included resources (files within automation package)
        List<Resource> resources = getPackageResources(currentAutomationPackage.getId());
        for (Resource resource : resources) {
            try {
                // included resources are only used within the automation package (not shared between several packages, so they can be simply deleted)
                log.debug("Remove the resource linked with AP '{}':{}", currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), resource.getId().toHexString());
                resourceManager.deleteResource(resource.getId().toString());
            } catch (Exception e) {
                log.error("Error while deleting resource {} for automation package {}",
                        resource.getId().toString(), currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), e
                );
            }
        }

        // 2. main resources (AP file and AP lib file)
        deleteMainApResourceIfPossible(currentAutomationPackage, newAutomationPackage, currentAutomationPackage.getAutomationPackageResource(), writeAccessPredicate);
        deleteMainApResourceIfPossible(currentAutomationPackage, newAutomationPackage, currentAutomationPackage.getAutomationPackageLibraryResource(), writeAccessPredicate);
    }

    private void deleteMainApResourceIfPossible(AutomationPackage currentAutomationPackage, AutomationPackage newAutomationPackage, String apResourceToCheck, ObjectPredicate writeAccessPredicate) {
        try {
            if (FileResolver.isResource(apResourceToCheck)) {
                boolean canBeDeleted = true;
                if (newAutomationPackage != null && (Objects.equals(newAutomationPackage.getAutomationPackageResource(), apResourceToCheck) || Objects.equals(newAutomationPackage.getAutomationPackageLibraryResource(), apResourceToCheck))) {
                    log.info("Resource {} cannot be deleted, because it is reused in new automation package: {}", apResourceToCheck, newAutomationPackage.getAttribute(AbstractOrganizableObject.NAME));
                    canBeDeleted = false;
                }

                if (canBeDeleted) {
                    String resourceId = FileResolver.resolveResourceId(apResourceToCheck);
                    Set<ObjectId> otherApsWithSameResource = linkedAutomationPackagesFinder.findAutomationPackagesByResourceId(resourceId, List.of(currentAutomationPackage.getId()));
                    if (!otherApsWithSameResource.isEmpty()) {
                        log.info("Resource {} cannot be deleted, because it is reused in other automation packages: {}", apResourceToCheck, otherApsWithSameResource);
                        canBeDeleted = false;
                    }

                    if (canBeDeleted) {
                        Resource resource = resourceManager.getResource(resourceId);
                        if (resource != null) {
                            if (writeAccessPredicate.test(resource)) {
                                log.debug("Remove the resource linked with AP '{}':{}", currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), apResourceToCheck);
                                resourceManager.deleteResource(resourceId);
                            } else {
                                log.debug("The resource linked with AP '{}':{} is not writable and won't be deleted with the package", currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), apResourceToCheck);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while deleting resource {} for automation package {}",
                    apResourceToCheck, currentAutomationPackage.getAttribute(AbstractOrganizableObject.NAME), e
            );
        }
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

    protected void checkAccess(AutomationPackage automationPackage, ObjectPredicate writeAccessPredicate) throws AutomationPackageAccessException {
        if (writeAccessPredicate != null) {
            if (!writeAccessPredicate.test(automationPackage)) {
                throw new AutomationPackageAccessException(automationPackage, "You're not allowed to edit this automation package from within this context");
            }
        }
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

    public void setProvidersResolver(DefaultProvidersResolver providersResolver) {
        this.providersResolver = providersResolver;
    }

    public AutomationPackageMavenConfig getMavenConfig(ObjectPredicate objectPredicate) {
        return mavenConfigProvider == null ? null : mavenConfigProvider.getConfig(objectPredicate);
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

    public interface AutomationPackageProvidersResolver {

        AutomationPackageArchiveProvider getAutomationPackageArchiveProvider(AutomationPackageFileSource apFileSource,
                                                                             ObjectPredicate predicate,
                                                                             AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider,
                                                                             AutomationPackageLibraryProvider apLibraryProvider) throws AutomationPackageReadingException;

        AutomationPackageLibraryProvider getAutomationPackageLibraryProvider(AutomationPackageFileSource apLibrarySource,
                                                                             ObjectPredicate predicate,
                                                                             AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) throws AutomationPackageReadingException;
    }

    public static class DefaultProvidersResolver implements AutomationPackageProvidersResolver {

        private final AutomationPackageReaderRegistry apReaderRegistry;
        private final ResourceManager resourceManager;

        public DefaultProvidersResolver(AutomationPackageReaderRegistry apReaderRegistry, ResourceManager resourceManager) {
            this.apReaderRegistry = apReaderRegistry;
            this.resourceManager = resourceManager;
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
                } else if (apFileSource.getMode() == AutomationPackageFileSource.Mode.RESOURCE_ID) {
                    return new AutomationPackageFromResourceIdProvider(apReaderRegistry, resourceManager, apFileSource.getResourceId(), apLibraryProvider, predicate);
                } else if (apFileSource.getMode() == AutomationPackageFileSource.Mode.EMPTY) {
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
            return new AutomationPackageFromMavenProvider(apReaderRegistry, mavenConfigProvider.getConfig(predicate),
                    apFileSource.getMavenArtifactIdentifier(), apLibraryProvider, resourceManager, predicate);
        }

        @Override
        public AutomationPackageLibraryProvider getAutomationPackageLibraryProvider(AutomationPackageFileSource apLibrarySource,
                                                                                    ObjectPredicate predicate,
                                                                                    AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) throws AutomationPackageReadingException {
            if (apLibrarySource != null) {
                if (apLibrarySource.getMode() == AutomationPackageFileSource.Mode.MAVEN) {
                    return createAutomationPackageLibraryFromMavenProvider(apLibrarySource, predicate, mavenConfigProvider, resourceManager);
                } else if (apLibrarySource.getMode() == AutomationPackageFileSource.Mode.INPUT_STREAM) {
                    return new AutomationPackageLibraryFromInputStreamProvider(apLibrarySource.getInputStream(), apLibrarySource.getFileName());
                } else if(apLibrarySource.getMode() == AutomationPackageFileSource.Mode.RESOURCE_ID){
                    return new AutomationPackageLibraryFromResourceIdProvider(resourceManager, apLibrarySource.getResourceId(), predicate);
                } else if(apLibrarySource.getMode() == AutomationPackageFileSource.Mode.EMPTY){
                    return new NoAutomationPackageLibraryProvider();
                } else {
                    throw new AutomationPackageManagerException("Unsupported mode for automation package library source: " + apLibrarySource.getMode());
                }
            } else {
                return new NoAutomationPackageLibraryProvider();
            }
        }

        protected AutomationPackageLibraryFromMavenProvider createAutomationPackageLibraryFromMavenProvider(AutomationPackageFileSource apLibrarySource, ObjectPredicate predicate, AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider, ResourceManager resourceManager) throws AutomationPackageReadingException {
            return new AutomationPackageLibraryFromMavenProvider(mavenConfigProvider.getConfig(predicate), apLibrarySource.getMavenArtifactIdentifier(), resourceManager, predicate);
        }
    }

}
