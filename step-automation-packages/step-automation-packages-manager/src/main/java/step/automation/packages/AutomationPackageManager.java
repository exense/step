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
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.InMemoryAutomationPackageAccessorImpl;
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
import step.resources.*;

import java.io.File;
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
                                     AutomationPackageLocks automationPackageLocks) {
        this.automationPackageAccessor = automationPackageAccessor;

        this.functionManager = functionManager;
        this.functionAccessor = functionAccessor;
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
                new AutomationPackageLocks(DEFAULT_READLOCK_TIMEOUT_SECONDS)
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

        ResourceManager resourceManager1 = new LocalResourceManagerImpl(new File("resources_" + isolatedContextId.toString()));
        InMemoryFunctionAccessorImpl inMemoryFunctionRepository = new InMemoryFunctionAccessorImpl();
        LayeredFunctionAccessor layeredFunctionAccessor = new LayeredFunctionAccessor(List.of(inMemoryFunctionRepository, mainFunctionAccessor));

        Map<String, Object> extensions = new HashMap<>();
        hookRegistry.onIsolatedAutomationPackageManagerCreate(extensions);
        AutomationPackageManager automationPackageManager = new AutomationPackageManager(
                AutomationPackageOperationMode.ISOLATED, new InMemoryAutomationPackageAccessorImpl(),
                new FunctionManagerImpl(layeredFunctionAccessor, functionTypeRegistry),
                layeredFunctionAccessor,
                new InMemoryPlanAccessor(),
                resourceManager1,
                extensions,
                hookRegistry, reader,
                new AutomationPackageLocks(DEFAULT_READLOCK_TIMEOUT_SECONDS)
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
                                                                              AutomationPackageLocks locks) {
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
                locks
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

    public void removeAutomationPackage(ObjectId id, ObjectPredicate objectPredicate) {
        AutomationPackage automationPackage = getAutomationPackageById(id, objectPredicate);
        String automationPackageId = automationPackage.getId().toHexString();
        if (automationPackageLocks.tryWriteLock(automationPackageId)) {
            try {
                deleteAutomationPackageEntities(automationPackage);
                automationPackageAccessor.remove(automationPackage.getId());
                log.info("Automation package ({}) has been removed", id);
            } finally {
                automationPackageLocks.releaseAndRemoveLock(automationPackageId);
            }
        } else {
            throw new AutomationPackageManagerException("Automation package cannot be removed while executions using it are running.");
        }
    }

    protected void deleteAutomationPackageEntities(AutomationPackage automationPackage) {
        deleteFunctions(automationPackage);
        deletePlans(automationPackage);
        // schedules will be deleted in deleteAdditionalData via hooks
        deleteResources(automationPackage);
        deleteAdditionalData(automationPackage, new AutomationPackageContext(operationMode, resourceManager, null,  null,null, extensions));
    }

    /**
     * Creates the new automation package. The exception will be thrown, if the package with the same name already exists.
     *
     * @param packageStream   the package content
     * @param fileName        the original name of file with automation package
     * @param enricher        the enricher used to fill all stored objects (for instance, with product id for multitenant application)
     * @param objectPredicate the filter for automation package
     * @return the id of created package
     * @throws AutomationPackageManagerException
     */
    public ObjectId createAutomationPackage(InputStream packageStream, String fileName, String apVersion, String activationExpr, ObjectEnricher enricher, ObjectPredicate objectPredicate) throws AutomationPackageManagerException {
        return createOrUpdateAutomationPackage(false, true, null, packageStream, fileName, apVersion, activationExpr, enricher, objectPredicate, false).getId();
    }

    /**
     * Creates new or updates the existing automation package
     *
     * @param allowUpdate     whether update existing package is allowed
     * @param allowCreate     whether create new package is allowed
     * @param explicitOldId   the explicit package id to be updated (if null, the id will be automatically resolved by package name from packageStream)
     * @param fileName        the original name of file with automation package
     * @param enricher        the enricher used to fill all stored objects (for instance, with product id for multitenant application)
     * @param objectPredicate the filter for automation package
     * @return the id of created/updated package
     */
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(boolean allowUpdate, boolean allowCreate, ObjectId explicitOldId,
                                                                         InputStream inputStream, String fileName, String apVersion, String activationExpr,
                                                                         ObjectEnricher enricher, ObjectPredicate objectPredicate, boolean async) throws AutomationPackageManagerException {
        try {
            try (AutomationPackageArchiveProvider provider = new AutomationPackageFromInputStreamProvider(inputStream, fileName)) {
                return createOrUpdateAutomationPackage(allowUpdate, allowCreate, explicitOldId, provider, apVersion, activationExpr, false, enricher, objectPredicate, async);
            }
        } catch (IOException | AutomationPackageReadingException ex) {
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
     * @return the id of created/updated package
     */
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(boolean allowUpdate, boolean allowCreate, ObjectId explicitOldId,
                                                                         AutomationPackageArchiveProvider automationPackageProvider, String apVersion, String activationExpr,
                                                                         boolean isLocalPackage, ObjectEnricher enricher, ObjectPredicate objectPredicate, boolean async) {
        AutomationPackageArchive automationPackageArchive;
        AutomationPackageContent packageContent;

        AutomationPackage newPackage = null;

        try {
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

            // keep old package id
            newPackage = createNewInstance(automationPackageArchive.getOriginalFileName(), packageContent, apVersion, activationExpr, oldPackage, enricher);

            // prepare staging collections
            AutomationPackageStaging staging = createStaging();
            List<ObjectEnricher> enrichers = new ArrayList<>();
            if (enricher != null) {
                enrichers.add(enricher);
            }
            enrichers.add(new AutomationPackageLinkEnricher(newPackage.getId().toString()));

            ObjectEnricher enricherForIncludedEntities = ObjectEnricherComposer.compose(enrichers);
            fillStaging(staging, packageContent, oldPackage, enricherForIncludedEntities, automationPackageArchive, activationExpr);

            // persist and activate automation package
            log.debug("Updating automation package, old package is " + ((oldPackage == null) ? "null" : "not null" + ", async: " + async));
            boolean immediateWriteLock = tryObtainImmediateWriteLock(newPackage);
            try {
                if (oldPackage == null || !async || immediateWriteLock) {
                    //If not async or if it's a new package, we synchronously wait on a write lock and update
                    log.info("Updating the automation package " + newPackage.getId().toString() + " synchronously, any running executions on this package will delay the update.");
                    ObjectId result = updateAutomationPackage(oldPackage, newPackage, packageContent, staging, enricherForIncludedEntities, immediateWriteLock, automationPackageArchive);
                    return new AutomationPackageUpdateResult(oldPackage == null ? AutomationPackageUpdateStatus.CREATED : AutomationPackageUpdateStatus.UPDATED, result);
                } else {
                    // async update
                    log.info("Updating the automation package " + newPackage.getId().toString() + " asynchronously due to running execution(s).");
                    newPackage.setStatus(AutomationPackageStatus.DELAYED_UPDATE);
                    automationPackageAccessor.save(newPackage);
                    AutomationPackage finalNewPackage = newPackage;
                    delayedUpdateExecutor.submit(() -> {
                        try {
                            updateAutomationPackage(oldPackage, finalNewPackage, packageContent, staging, enricherForIncludedEntities, false, automationPackageArchive);
                        } catch (Exception e) {
                            handleExceptionOnPackageUpdate(finalNewPackage);
                            log.error("Exception on delayed AP update", e);
                        }
                    });
                    return new AutomationPackageUpdateResult(AutomationPackageUpdateStatus.UPDATE_DELAYED, newPackage.getId());
                }
            } finally {
                if (immediateWriteLock) {
                    releaseWriteLock(newPackage);
                }
            }
        } catch (Exception ex) {
            handleExceptionOnPackageUpdate(newPackage);
            throw ex;
        }
    }

    public Map<String, List<? extends AbstractOrganizableObject>> getAllEntities(ObjectId automationPackageId) {
        Map<String, List<? extends AbstractOrganizableObject>> result = new HashMap<>();
        List<Plan> packagePlans = getPackagePlans(automationPackageId);
        result.put(PLANS_ENTITY_NAME, packagePlans);
        List<Function> packageFunctions = getPackageFunctions(automationPackageId);
        result.put(AutomationPackageKeyword.KEYWORDS_ENTITY_NAME, packageFunctions);
        List<String> allHooksNames = automationPackageHookRegistry.getOrderedHookFieldNames();
        for (String hookName : allHooksNames) {
            AutomationPackageHook<?> hook = automationPackageHookRegistry.getHook(hookName);
            result.putAll(hook.getEntitiesForAutomationPackage(
                            automationPackageId,
                            new AutomationPackageContext(operationMode, resourceManager, null, null, null, extensions)
                    )
            );
        }
        return result;
    }

    private ObjectId updateAutomationPackage(AutomationPackage oldPackage, AutomationPackage newPackage,
                                             AutomationPackageContent packageContent, AutomationPackageStaging staging, ObjectEnricher enricherForIncludedEntities,
                                             boolean alreadyLocked, AutomationPackageArchive automationPackageArchive) {
        try {
            //If not already locked (i.e. was not able to acquire an immediate write lock)
            if (!alreadyLocked) {
                log.info("Delaying update of the automation package " + newPackage.getId().toString() + " due to running execution(s) using this package.");
                getWriteLock(newPackage);
                log.info("Executions completed, proceeding with the update of the automation package " + newPackage.getId().toString());
            }
            // delete old package entities
            if (oldPackage != null) {
                deleteAutomationPackageEntities(oldPackage);
            }
            // persist all staged entities
            persistStagedEntities(staging, enricherForIncludedEntities, automationPackageArchive, packageContent);
            ObjectId result = automationPackageAccessor.save(newPackage).getId();
            logAfterSave(staging, oldPackage, newPackage);
            return result;
        } finally {
            if (!alreadyLocked) {
                releaseWriteLock(newPackage); //only release if lock was acquired in this method
            }
            //Clear delayed status
            newPackage.setStatus(null);
            automationPackageAccessor.save(newPackage);
        }
    }

    private void handleExceptionOnPackageUpdate(AutomationPackage automationPackage) {
        // cleanup created resources
        try {
            if (automationPackage != null) {
                List<Resource> resources = resourceManager.findManyByCriteria(AutomationPackageEntity.getAutomationPackageIdCriteria(automationPackage.getId()));
                for (Resource resource : resources) {
                    resourceManager.deleteResource(resource.getId().toString());
                }
            }
        } catch (Exception e) {
            log.warn("Cannot cleanup resource", e);
        }
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

        for (Map.Entry<String, List<?>> additionalObjects : staging.getAdditionalObjects().entrySet()) {
            message.append(String.format(" %s: %s.", additionalObjects.getKey(), additionalObjects.getValue().size()));
        }

        log.info(message.toString());
    }

    protected AutomationPackageStaging createStaging(){
        return new AutomationPackageStaging();
    }

    protected void fillStaging(AutomationPackageStaging staging, AutomationPackageContent packageContent, AutomationPackage oldPackage, ObjectEnricher enricherForIncludedEntities,
                               AutomationPackageArchive automationPackageArchive, String evaluationExpression) {
        staging.getPlans().addAll(preparePlansStaging(packageContent, automationPackageArchive, oldPackage, enricherForIncludedEntities, staging.getResourceManager(), evaluationExpression));
        staging.getFunctions().addAll(prepareFunctionsStaging(automationPackageArchive, packageContent, enricherForIncludedEntities, oldPackage, staging.getResourceManager(), evaluationExpression));

        List<HookEntry> hookEntries = packageContent.getAdditionalData().entrySet().stream().map(e -> new HookEntry(e.getKey(), e.getValue())).collect(Collectors.toList());
        List<String> orderedEntryNames = automationPackageHookRegistry.getOrderedHookFieldNames();
        // sort the hook entries according to their "natural" order defined by the registry
        hookEntries.sort(Comparator.comparingInt(he -> orderedEntryNames.indexOf(he.fieldName)));

        for (HookEntry hookEntry : hookEntries) {
            boolean hooked =  automationPackageHookRegistry.onPrepareStaging(
                    hookEntry.fieldName,
                    new AutomationPackageContext(operationMode, staging.getResourceManager(), automationPackageArchive, packageContent, enricherForIncludedEntities, extensions),
                    packageContent,
                    hookEntry.values,
                    oldPackage, staging);

            if (!hooked) {
                log.warn("Additional field in automation package has been ignored and skipped: " + hookEntry.fieldName);
            }
        }
    }

    protected void persistStagedEntities(AutomationPackageStaging staging,
                                         ObjectEnricher objectEnricher,
                                         AutomationPackageArchive automationPackageArchive,
                                         AutomationPackageContent packageContent) {
        List<Resource> stagingResources = staging.getResourceManager().findManyByCriteria(null);
        try {
            for (Resource resource: stagingResources) {
                resourceManager.copyResource(resource, staging.getResourceManager());
            }
        } catch (IOException | SimilarResourceExistingException | InvalidResourceFormatException e) {
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
        List<HookEntry> hookEntries = staging.getAdditionalObjects().entrySet().stream().map(e -> new HookEntry(e.getKey(), e.getValue())).collect(Collectors.toList());
        for (HookEntry hookEntry : hookEntries) {
            boolean hooked = automationPackageHookRegistry.onCreate(
                    hookEntry.fieldName, hookEntry.values,
                    new AutomationPackageContext(operationMode, resourceManager, automationPackageArchive, packageContent, objectEnricher, extensions)
            );
            if (!hooked) {
                log.warn("Additional field in automation package has been ignored and skipped: " + hookEntry.fieldName);
            }
        }
    }

    public <T extends AbstractOrganizableObject & EnricheableObject> void fillEntities(List<T> entities, List<T> oldEntities, ObjectEnricher enricher) {
        Entity.reuseOldIds(entities, oldEntities);
        entities.forEach(enricher);
    }

    public void runExtensionsBeforeIsolatedExecution(AutomationPackage automationPackage, AbstractStepContext executionContext, Map<String, Object> apManagerExtensions, ImportResult importResult){
        automationPackageHookRegistry.beforeIsolatedExecution(automationPackage, executionContext, apManagerExtensions, importResult);
    }

    protected List<Plan> preparePlansStaging(AutomationPackageContent packageContent, AutomationPackageArchive automationPackageArchive,
                                             AutomationPackage oldPackage, ObjectEnricher enricher, ResourceManager stagingResourceManager,
                                             String evaluationExpression) {
        List<Plan> plans = packageContent.getPlans();
        AutomationPackagePlansAttributesApplier specialAttributesApplier = new AutomationPackagePlansAttributesApplier(stagingResourceManager);
        specialAttributesApplier.applySpecialAttributesToPlans(plans, automationPackageArchive, packageContent, enricher, extensions, operationMode);

        fillEntities(plans, oldPackage != null ? getPackagePlans(oldPackage.getId()) : new ArrayList<>(), enricher);
        if (evaluationExpression != null && !evaluationExpression.isEmpty()){
            for (Plan plan : plans) {
                plan.setActivationExpression(new Expression(evaluationExpression));
            }
        }
        return plans;
    }

    protected List<Function> prepareFunctionsStaging(AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent, ObjectEnricher enricher,
                                                     AutomationPackage oldPackage, ResourceManager stagingResourceManager, String evaluationExpression) {
        AutomationPackageContext apContext = new AutomationPackageContext(operationMode, stagingResourceManager, automationPackageArchive, packageContent, enricher, extensions);
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

    protected AutomationPackage createNewInstance(String fileName, AutomationPackageContent packageContent, String apVersion, String activationExpr, AutomationPackage oldPackage, ObjectEnricher enricher) {
        AutomationPackage newPackage = new AutomationPackage();

        // keep old id
        if (oldPackage != null) {
            newPackage.setId(oldPackage.getId());
        }
        newPackage.addAttribute(AbstractOrganizableObject.NAME, packageContent.getName());
        newPackage.addAttribute(AbstractOrganizableObject.VERSION, packageContent.getVersion());

        newPackage.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_FILE_NAME, fileName);
        if (activationExpr != null && !activationExpr.isEmpty()) {
            newPackage.setActivationExpression(new Expression(activationExpr));
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
        automationPackageHookRegistry.onAutomationPackageDelete(automationPackage, context, null);
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
