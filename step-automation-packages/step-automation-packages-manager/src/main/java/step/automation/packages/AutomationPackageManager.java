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
import step.automation.packages.hooks.AutomationPackageHookRegistry;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageSchedule;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectEnricherComposer;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.CronExclusion;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutiontTaskParameters;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.manager.FunctionManager;
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

public abstract class AutomationPackageManager {

    public static final int DEFAULT_READLOCK_TIMEOUT_SECONDS = 60;

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageManager.class);

    protected final AutomationPackageAccessor automationPackageAccessor;
    protected final FunctionManager functionManager;
    protected final FunctionAccessor functionAccessor;
    protected final PlanAccessor planAccessor;
    protected final ExecutionTaskAccessor executionTaskAccessor;
    protected final AbstractAutomationPackageReader<?> packageReader;

    protected final ResourceManager resourceManager;
    protected final AutomationPackageHookRegistry automationPackageHookRegistry;
    protected boolean isIsolated = false;
    protected final AutomationPackageLocks automationPackageLocks;

    private final ExecutorService delayedUpdateExecutor = Executors.newCachedThreadPool();

    /**
     * The automation package manager used to store/delete automation packages. To run the automation package in isolated
     * context please use the separate in-memory automation package manager created via
     * {@link AutomationPackageManager#createIsolated(ObjectId, FunctionTypeRegistry, FunctionAccessor)}
     */
    public AutomationPackageManager(AutomationPackageAccessor automationPackageAccessor,
                                    FunctionManager functionManager,
                                    FunctionAccessor functionAccessor,
                                    PlanAccessor planAccessor,
                                    ResourceManager resourceManager,
                                    ExecutionTaskAccessor executionTaskAccessor,
                                    AutomationPackageHookRegistry automationPackageHookRegistry,
                                    AbstractAutomationPackageReader<?> packageReader, AutomationPackageLocks automationPackageLocks) {
        this.automationPackageAccessor = automationPackageAccessor;

        this.functionManager = functionManager;
        this.functionAccessor = functionAccessor;
        IndexField indexField = new IndexField(getAutomationPackageTrackingField(), Order.ASC, String.class);
        this.functionAccessor.createIndexIfNeeded(indexField);

        this.planAccessor = planAccessor;
        this.planAccessor.createIndexIfNeeded(indexField);

        this.executionTaskAccessor = executionTaskAccessor;
        this.executionTaskAccessor.createIndexIfNeeded(indexField);

        this.automationPackageHookRegistry = automationPackageHookRegistry;
        this.packageReader = packageReader;
        this.resourceManager = resourceManager;
        this.automationPackageLocks = automationPackageLocks;
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
    public abstract AutomationPackageManager createIsolated(ObjectId isolatedContextId, FunctionTypeRegistry functionTypeRegistry, FunctionAccessor mainFunctionAccessor);

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

    public AutomationPackage getAutomatonPackageById(ObjectId id) {
        return this.getAutomationPackageById(id, null);
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
        deleteSchedules(automationPackage);
        deleteResources(automationPackage);
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
    public ObjectId createAutomationPackage(InputStream packageStream, String fileName, ObjectEnricher enricher, ObjectPredicate objectPredicate) throws AutomationPackageManagerException {
        return createOrUpdateAutomationPackage(false, true, null, packageStream, fileName, enricher, objectPredicate, false).getId();
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
                                                                         InputStream inputStream, String fileName, ObjectEnricher enricher,
                                                                         ObjectPredicate objectPredicate, boolean async) throws AutomationPackageManagerException {
        try {
            try (AutomationPackageArchiveProvider provider = new AutomationPackageFromInputStreamProvider(inputStream, fileName)) {
                return createOrUpdateAutomationPackage(allowUpdate, allowCreate, explicitOldId, provider, false, enricher, objectPredicate, async);
            }
        } catch (IOException | AutomationPackageReadingException ex) {
            throw new AutomationPackageManagerException("Automation package cannot be created. Caused by: " + ex.getMessage(), ex);
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
                                                                         AutomationPackageArchiveProvider automationPackageProvider, boolean isLocalPackage,
                                                                         ObjectEnricher enricher, ObjectPredicate objectPredicate, boolean async) {
        AutomationPackageArchive automationPackageArchive;
        AutomationPackageContent packageContent;

        AutomationPackage newPackage = null;

        try {
            try {
                automationPackageArchive = automationPackageProvider.getAutomationPackageArchive();
                packageContent = readAutomationPackage(automationPackageArchive, isLocalPackage);
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
            newPackage = createNewInstance(automationPackageArchive.getOriginalFileName(), packageContent, oldPackage, enricher);

            // prepare staging collections
            Staging staging = createStaging();
            ObjectEnricher enricherForIncludedEntities = ObjectEnricherComposer.compose(Arrays.asList(enricher, new AutomationPackageLinkEnricher(newPackage.getId().toString())));
            fillStaging(staging, packageContent, oldPackage, enricherForIncludedEntities, automationPackageArchive);

            // persist and activate automation package
            log.debug("Updating automation package, old package is " + ((oldPackage == null) ? "null" : "not null" + ", async: " + async));
            boolean immediateWriteLock = tryObtainImmediateWriteLock(newPackage);
            try {
                if (oldPackage == null || !async || immediateWriteLock) {
                    //If not async or if it's a new package, we synchronously wait on a write lock and update
                    log.info("Updating the automation package " + newPackage.getId().toString() + " synchronously, any running executions on this package will delay the update.");
                    ObjectId result = updateAutomationPackage(oldPackage, newPackage, staging, enricherForIncludedEntities, immediateWriteLock);
                    return new AutomationPackageUpdateResult(oldPackage == null ? AutomationPackageUpdateStatus.CREATED : AutomationPackageUpdateStatus.UPDATED, result);
                } else {
                    // async update
                    log.info("Updating the automation package " + newPackage.getId().toString() + " asynchronously due to running execution(s).");
                    newPackage.setStatus(AutomationPackageStatus.DELAYED_UPDATE);
                    automationPackageAccessor.save(newPackage);
                    AutomationPackage finalNewPackage = newPackage;
                    delayedUpdateExecutor.submit(() -> {
                        try {
                            updateAutomationPackage(oldPackage, finalNewPackage, staging, enricherForIncludedEntities, false);
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

    private ObjectId updateAutomationPackage(AutomationPackage oldPackage, AutomationPackage newPackage,
                                             Staging staging, ObjectEnricher enricherForIncludedEntities,
                                             boolean alreadyLocked) {
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
            persistStagedEntities(staging, enricherForIncludedEntities);
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
                List<Resource> resources = resourceManager.findManyByCriteria(Map.of("customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID, automationPackage.getId().toString()));
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

    protected void logAfterSave(Staging staging, AutomationPackage oldPackage, AutomationPackage newPackage) {
        if (oldPackage != null) {
            log.info("Automation package has been updated ({}). Plans: {}. Functions: {}. Schedules: {}",
                    newPackage.getAttribute(AbstractOrganizableObject.NAME),
                    staging.plans.size(),
                    staging.functions.size(),
                    staging.taskParameters.size()
            );
        } else {
            log.info("New automation package saved ({}). Plans: {}. Functions: {}. Schedules: {}",
                    newPackage.getAttribute(AbstractOrganizableObject.NAME),
                    staging.plans.size(),
                    staging.functions.size(),
                    staging.taskParameters.size()
            );
        }
    }

    protected Staging createStaging(){
        return new Staging();
    }

    protected void fillStaging(Staging staging, AutomationPackageContent packageContent, AutomationPackage oldPackage, ObjectEnricher enricherForIncludedEntities, AutomationPackageArchive automationPackageArchive){
        staging.plans = preparePlansStaging(packageContent, automationPackageArchive, oldPackage, enricherForIncludedEntities, staging.resourceManager);
        staging.taskParameters = prepareExecutionTasksParamsStaging(enricherForIncludedEntities, packageContent, oldPackage, staging.plans);
        staging.functions = prepareFunctionsStaging(automationPackageArchive, packageContent, enricherForIncludedEntities, oldPackage, staging.resourceManager);
    }

    protected void persistStagedEntities(Staging staging,
                                         ObjectEnricher objectEnricher) {
        List<Resource> stagingResources = staging.resourceManager.findManyByCriteria(null);
        try {
            for (Resource resource: stagingResources) {
                resourceManager.copyResource(resource, staging.resourceManager);
            }
        } catch (IOException | SimilarResourceExistingException | InvalidResourceFormatException e) {
            throw new AutomationPackageManagerException("Unable to persist a resource in automation package", e);
        } finally {
            staging.resourceManager.cleanup();
        }

        try {
            for (Function completeFunction : staging.functions) {
                functionManager.saveFunction(completeFunction);
            }
        } catch (SetupFunctionException | FunctionTypeException e) {
            throw new AutomationPackageManagerException("Unable to persist a keyword in automation package", e);
        }

        for (Plan plan : staging.plans) {
            planAccessor.save(plan);
        }

        for (ExecutiontTaskParameters execTasksParameter : staging.taskParameters) {
            //make sure the execution parameter of the schedule are enriched too (required to execute in same project
            //as the schedule and populate event bindings
            objectEnricher.accept(execTasksParameter.getExecutionsParameters());
            if (! automationPackageHookRegistry.onCreate(execTasksParameter)) {
                executionTaskAccessor.save(execTasksParameter);
            }
        }

    }

    protected <T extends AbstractOrganizableObject & EnricheableObject> void fillEntities(List<T> entities, List<T> oldEntities, ObjectEnricher enricher) {
        Map<String, ObjectId> nameToIdMap = createNameToIdMap(oldEntities);

        for (T e : entities) {
            // keep old id
            ObjectId oldId = nameToIdMap.get(e.getAttribute(AbstractOrganizableObject.NAME));
            if (oldId != null) {
                e.setId(oldId);
            }

            if (enricher != null) {
                enricher.accept(e);
            }
        }
    }

    protected List<Plan> preparePlansStaging(AutomationPackageContent packageContent, AutomationPackageArchive automationPackageArchive,
                                             AutomationPackage oldPackage, ObjectEnricher enricher, ResourceManager stagingResourceManager) {
        List<Plan> plans = packageContent.getPlans();
        AutomationPackagePlansAttributesApplier specialAttributesApplier = new AutomationPackagePlansAttributesApplier(stagingResourceManager);
        specialAttributesApplier.applySpecialAttributesToPlans(plans, automationPackageArchive, enricher);

        fillEntities(plans, oldPackage != null ? getPackagePlans(oldPackage.getId()) : new ArrayList<>(), enricher);
        return plans;
    }

    protected List<Function> prepareFunctionsStaging(AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent, ObjectEnricher enricher, AutomationPackage oldPackage, ResourceManager stagingResourceManager) {
        AutomationPackageContext apContext = new AutomationPackageContext(stagingResourceManager, automationPackageArchive, enricher);
        List<Function> completeFunctions = packageContent.getKeywords().stream().map(keyword -> keyword.prepareKeyword(apContext)).collect(Collectors.toList());

        // get old functions with same name and reuse their ids
        List<Function> oldFunctions = oldPackage == null ? new ArrayList<>() : getPackageFunctions(oldPackage.getId());
        fillEntities(completeFunctions, oldFunctions, enricher);
        return completeFunctions;
    }

    protected List<ExecutiontTaskParameters> prepareExecutionTasksParamsStaging(ObjectEnricher enricher, AutomationPackageContent packageContent, AutomationPackage oldPackage, List<Plan> plansStaging) {
        List<ExecutiontTaskParameters> completeExecTasksParameters = new ArrayList<>();
        for (AutomationPackageSchedule schedule : packageContent.getSchedules()) {
            ExecutiontTaskParameters execTaskParameters = new ExecutiontTaskParameters();

            execTaskParameters.setActive(schedule.getActive() == null || schedule.getActive());
            execTaskParameters.addAttribute(AbstractOrganizableObject.NAME, schedule.getName());
            execTaskParameters.setCronExpression(schedule.getCron());
            List<String> cronExclusionsAsStrings = schedule.getCronExclusions();
            if (cronExclusionsAsStrings != null && cronExclusionsAsStrings.size() > 1) {
                List<CronExclusion> cronExclusions = cronExclusionsAsStrings.stream().map(s -> new CronExclusion(s, "")).collect(Collectors.toList());
                execTaskParameters.setCronExclusions(cronExclusions);
            }
            String assertionPlanName = schedule.getAssertionPlanName();
            if (assertionPlanName != null && !assertionPlanName.isEmpty()) {
                Plan assertionPlan = lookupPlanByName(plansStaging, assertionPlanName);
                if (assertionPlan == null) {
                    throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                            ". No assertion plan with '" + assertionPlanName + "' name found for schedule " + schedule.getName());
                }
                execTaskParameters.setAssertionPlan(assertionPlan.getId());
            }

            String planNameFromSchedule = schedule.getPlanName();
            if (planNameFromSchedule == null || planNameFromSchedule.isEmpty()) {
                throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                        ". Plan name is not defined for schedule " + schedule.getName());
            }

            Plan plan = lookupPlanByName(plansStaging, planNameFromSchedule);
            if (plan == null) {
                throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                        ". No plan with '" + planNameFromSchedule + "' name found for schedule " + schedule.getName());
            }

            RepositoryObjectReference repositoryObjectReference = new RepositoryObjectReference(
                    RepositoryObjectReference.LOCAL_REPOSITORY_ID, Map.of(RepositoryObjectReference.PLAN_ID, plan.getId().toString())
            );
            ExecutionParameters executionParameters = new ExecutionParameters(repositoryObjectReference, plan.getAttribute(AbstractOrganizableObject.NAME),
                    schedule.getExecutionParameters());
            execTaskParameters.setExecutionsParameters(executionParameters);
            completeExecTasksParameters.add(execTaskParameters);
        }
        fillEntities(completeExecTasksParameters, oldPackage != null ? getPackageSchedules(oldPackage.getId()) : new ArrayList<>(), enricher);
        return completeExecTasksParameters;
    }

    private Plan lookupPlanByName(List<Plan> plansStaging, String planName) {
        Plan plan = plansStaging.stream().filter(p -> Objects.equals(p.getAttribute(AbstractOrganizableObject.NAME), planName)).findFirst().orElse(null);
        if (plan == null) {
            // schedule can reference the existing persisted plan (not defined inside the automation package)
            plan = planAccessor.findByAttributes(Map.of(AbstractOrganizableObject.NAME, planName));
        }
        return plan;
    }

    private Map<String, ObjectId> createNameToIdMap(List<? extends AbstractOrganizableObject> objects) {
        Map<String, ObjectId> nameToIdMap = new HashMap<>();
        for (AbstractOrganizableObject o : objects) {
            String name = o.getAttribute(AbstractOrganizableObject.NAME);
            if (name != null) {
                nameToIdMap.put(name, o.getId());
            }
        }
        return nameToIdMap;
    }

    protected AutomationPackage createNewInstance(String fileName, AutomationPackageContent packageContent, AutomationPackage oldPackage, ObjectEnricher enricher) {
        AutomationPackage newPackage = new AutomationPackage();

        // keep old id
        if (oldPackage != null) {
            newPackage.setId(oldPackage.getId());
        }
        newPackage.addAttribute(AbstractOrganizableObject.NAME, packageContent.getName());
        newPackage.addAttribute(AbstractOrganizableObject.VERSION, packageContent.getVersion());

        newPackage.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_FILE_NAME, fileName);
        if (enricher != null) {
            enricher.accept(newPackage);
        }
        return newPackage;
    }

    protected AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive, boolean isLocalPackage) throws AutomationPackageReadingException {
        AutomationPackageContent packageContent;
        packageContent = packageReader.readAutomationPackage(automationPackageArchive, isLocalPackage);
        if (packageContent == null) {
            throw new AutomationPackageManagerException("Automation package descriptor is missing, allowed names: " + METADATA_FILES);
        } else if (packageContent.getName() == null || packageContent.getName().isEmpty()) {
            throw new AutomationPackageManagerException("Automation package name is missing");
        }
        return packageContent;
    }

    protected List<ExecutiontTaskParameters> deleteSchedules(AutomationPackage automationPackage) {
        List<ExecutiontTaskParameters> schedules = getPackageSchedules(automationPackage.getId());
        schedules.forEach(schedule -> {
            try {
                if (!automationPackageHookRegistry.onDelete(schedule)) {
                    executionTaskAccessor.remove(schedule.getId());
                }
            } catch (Exception e) {
                log.error("Error while deleting task {} for automation package {}",
                        schedule.getId().toString(), automationPackage.getAttribute(AbstractOrganizableObject.NAME), e
                );
            }
        });
        return schedules;
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

    protected List<Function> getFunctionsByCriteria(Map<String, String> criteria) {
        return functionAccessor.findManyByCriteria(criteria).collect(Collectors.toList());
    }

    protected List<Function> getFunctionsByAttributes(Map<String, String> criteria) {
        return StreamSupport.stream(functionAccessor.findManyByAttributes(criteria), false).collect(Collectors.toList());
    }

    public List<Function> getPackageFunctions(ObjectId automationPackageId) {
        return getFunctionsByCriteria(getAutomationPackageIdCriteria(automationPackageId));
    }

    protected List<Resource> getResourcesByCriteria(Map<String, String> criteria) {
        return resourceManager.findManyByCriteria(criteria);
    }

    public List<Resource> getPackageResources(ObjectId automationPackageId) {
        return getResourcesByCriteria(getAutomationPackageIdCriteria(automationPackageId));
    }

    protected Map<String, String> getAutomationPackageIdCriteria(ObjectId automationPackageId) {
        return Map.of(getAutomationPackageTrackingField(), automationPackageId.toString());
    }

    protected String getAutomationPackageTrackingField() {
        return "customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID;
    }

    public List<Plan> getPackagePlans(ObjectId automationPackageId) {
        return planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(automationPackageId)).collect(Collectors.toList());
    }

    protected List<ExecutiontTaskParameters> getPackageSchedules(ObjectId automationPackageId) {
        return executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(automationPackageId)).collect(Collectors.toList());
    }

    public AbstractAutomationPackageReader<?> getPackageReader() {
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

    public ExecutionTaskAccessor getExecutionTaskAccessor() {
        return executionTaskAccessor;
    }

    public void cleanup() {
        if (isIsolated) {
            this.resourceManager.cleanup();
        } else {
            log.info("Skip automation package cleanup. Cleanup is only supported for isolated (in-memory) automation package manager");
        }
    }

    protected static class Staging {
        List<Plan> plans = new ArrayList<>();
        List<ExecutiontTaskParameters> taskParameters = new ArrayList<>();
        List<Function> functions = new ArrayList<>();
        ResourceManager resourceManager = new LocalResourceManagerImpl(new File("ap_staging_resources_" + new ObjectId()));
    }

}
