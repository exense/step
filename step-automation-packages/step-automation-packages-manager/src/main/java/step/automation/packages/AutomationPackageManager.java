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

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.InMemoryAutomationPackageAccessorImpl;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.model.AutomationPackageSchedule;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectEnricherComposer;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.accessor.LayeredFunctionAccessor;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeException;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.SetupFunctionException;
import step.resources.LocalResourceManagerImpl;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static step.automation.packages.AutomationPackageArchive.METADATA_FILES;

public class AutomationPackageManager {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageManager.class);

    private final AutomationPackageAccessor automationPackageAccessor;
    private final FunctionManager functionManager;
    private final FunctionAccessor functionAccessor;
    private final PlanAccessor planAccessor;
    private final ExecutionTaskAccessor executionTaskAccessor;
    private final ExecutionScheduler executionScheduler;
    private final AutomationPackageReader packageReader;
    private final AutomationPackageKeywordsAttributesApplier keywordsAttributesApplier;
    private final ResourceManager resourceManager;
    private boolean isIsolated = false;

    /**
     * The automation package manager used to store/delete automation packages. To run the automation package in isolated
     * context please use the separate in-memory automation package manager created via
     * {@link AutomationPackageManager#createIsolatedAutomationPackageManager(ObjectId, FunctionTypeRegistry, FunctionAccessor)}
     */
    public AutomationPackageManager(AutomationPackageAccessor automationPackageAccessor,
                                    FunctionManager functionManager,
                                    FunctionAccessor functionAccessor,
                                    PlanAccessor planAccessor,
                                    ResourceManager resourceManager,
                                    ExecutionTaskAccessor executionTaskAccessor,
                                    ExecutionScheduler executionScheduler) {
        this.automationPackageAccessor = automationPackageAccessor;

        this.functionManager = functionManager;
        this.functionAccessor = functionAccessor;
        this.functionAccessor.createIndexIfNeeded(getAutomationPackageTrackingField());

        this.planAccessor = planAccessor;
        this.planAccessor.createIndexIfNeeded(getAutomationPackageTrackingField());

        this.executionTaskAccessor = executionTaskAccessor;
        this.executionTaskAccessor.createIndexIfNeeded(getAutomationPackageTrackingField());

        // TODO: avoid executionScheduler in automation package manager
        this.executionScheduler = executionScheduler;
        this.packageReader = new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH);
        this.resourceManager = resourceManager;
        this.keywordsAttributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);
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
                                                                                  FunctionAccessor mainFunctionAccessor) {
        InMemoryFunctionAccessorImpl inMemoryFunctionRepository = new InMemoryFunctionAccessorImpl();
        LayeredFunctionAccessor layeredFunctionAccessor = new LayeredFunctionAccessor(List.of(inMemoryFunctionRepository, mainFunctionAccessor));

        AutomationPackageManager automationPackageManager = new AutomationPackageManager(
                new InMemoryAutomationPackageAccessorImpl(),
                new FunctionManagerImpl(layeredFunctionAccessor, functionTypeRegistry),
                layeredFunctionAccessor,
                new InMemoryPlanAccessor(),
                new LocalResourceManagerImpl(new File("resources", isolatedContextId.toString())),
                new InMemoryExecutionTaskAccessor(),
                null
        );
        automationPackageManager.isIsolated = true;
        return automationPackageManager;
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
        deleteAutomationPackageEntities(automationPackage);
        automationPackageAccessor.remove(automationPackage.getId());
        log.info("Automation package ({}) has been removed", id);
    }

    private void deleteAutomationPackageEntities(AutomationPackage automationPackage) {
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
        return createOrUpdateAutomationPackage(false, true, null, packageStream, fileName, enricher, objectPredicate).getId();
    }

    /**
     * Creates new or updates the existing automation package
     *
     * @param allowUpdate     whether update existing package is allowed
     * @param allowCreate     whether create new package is allowed
     * @param explicitOldId   the explicit package id to be updated (if null, the id will be automatically resolved by package name from packageStream)
     * @param packageStream   the package content
     * @param fileName        the original name of file with automation package
     * @param enricher        the enricher used to fill all stored objects (for instance, with product id for multitenant application)
     * @param objectPredicate the filter for automation package
     * @return the id of created/updated package
     * @throws SetupFunctionException
     * @throws FunctionTypeException
     */
    public PackageUpdateResult createOrUpdateAutomationPackage(boolean allowUpdate, boolean allowCreate, ObjectId explicitOldId, InputStream packageStream, String fileName, ObjectEnricher enricher, ObjectPredicate objectPredicate) {
        AutomationPackageArchive automationPackageArchive;
        AutomationPackageContent packageContent;

        AutomationPackage newPackage = null;

        // store automation package into temp file
        File automationPackageFile = null;
        try {
            automationPackageFile = stream2file(packageStream, fileName);
        } catch (Exception ex) {
            throw new AutomationPackageManagerException("Unable to store automation package file");
        }

        try {
            try {
                automationPackageArchive = new AutomationPackageArchive(automationPackageFile, fileName);
                packageContent = readAutomationPackage(automationPackageArchive);
            } catch (AutomationPackageReadingException e) {
                throw new AutomationPackageManagerException("Unable to read automation package", e);
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
            }if (!allowUpdate && oldPackage != null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' already exists");
            }if (!allowCreate && oldPackage == null) {
            throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' doesn't exist");
        }

            // keep old package id
            newPackage = createNewInstance(fileName, packageContent, oldPackage, enricher);

            // prepare staging collections
            ObjectEnricher enricherForIncludedEntities = ObjectEnricherComposer.compose(Arrays.asList(enricher, new AutomationPackageLinkEnricher(newPackage.getId().toString())));
            List<Plan> completePlans = preparePlansStaging(packageContent, oldPackage, enricherForIncludedEntities);
            List<ExecutiontTaskParameters> completeExecTasksParameters = prepareExecutionTasksParamsStaging(enricherForIncludedEntities, packageContent, oldPackage, completePlans);
            List<Function> completeFunctions = prepareFunctionsStaging(newPackage, automationPackageArchive, packageContent, enricherForIncludedEntities, oldPackage);

            // delete old package entities
            if (oldPackage != null) {
                deleteAutomationPackageEntities(oldPackage);
            }

            // persist all staged entities
            persistStagedEntities(completeExecTasksParameters, completeFunctions, completePlans);

            // save automation package metadata
            ObjectId result = automationPackageAccessor.save(newPackage).getId();

            if (oldPackage != null) {
                log.info("Automation package has been updated ({}). Plans: {}. Functions: {}. Schedules: {}", newPackage.getAttribute(AbstractOrganizableObject.NAME), completePlans.size(), completeFunctions.size(), completeExecTasksParameters.size());
            } else {
                log.info("New automation package saved ({}). Plans: {}. Functions: {}. Schedules: {}", newPackage.getAttribute(AbstractOrganizableObject.NAME), completePlans.size(), completeFunctions.size(), completeExecTasksParameters.size());
            }
            return new PackageUpdateResult(oldPackage == null ? PackageUpdateStatus.CREATED : PackageUpdateStatus.UPDATED, result);
        } catch (Exception ex) {
            // cleanup created resources
            try {
                if (newPackage != null) {
                    List<Resource> resources = resourceManager.findManyByCriteria(Map.of("customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID, newPackage.getId().toString()));
                    for (Resource resource : resources) {
                        resourceManager.deleteResource(resource.getId().toString());
                    }
                }
            } catch (Exception e) {
                log.warn("Cannot cleanup resource", e);
            }
            throw ex;
        } finally {
            // cleanup temp file
            try {
                if (automationPackageFile.exists()) {
                    automationPackageFile.delete();
                }
            } catch (Exception e) {
                log.warn("Cannot cleanup temp file {}", automationPackageFile.getName(), e);
            }
        }
    }

    private void persistStagedEntities(List<ExecutiontTaskParameters> completeExecTasksParameters, List<Function> completeFunctions, List<Plan> completePlans) {
        try {
            for (Function completeFunction : completeFunctions) {
                functionManager.saveFunction(completeFunction);
            }
        } catch (SetupFunctionException | FunctionTypeException e) {
            throw new AutomationPackageManagerException("Unable to persist a keyword in automation package", e);
        }

        for (Plan plan : completePlans) {
            planAccessor.save(plan);
        }

        for (ExecutiontTaskParameters execTasksParameter : completeExecTasksParameters) {
            if (executionScheduler != null) {
                // TODO: move this to a dedicated class as part of SED-2594
                executionScheduler.addExecutionTask(execTasksParameter, false);
            } else {
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

    protected List<Plan> preparePlansStaging(AutomationPackageContent packageContent, AutomationPackage oldPackage, ObjectEnricher enricher) {
        List<Plan> plans = packageContent.getPlans();
        fillEntities(plans, oldPackage != null ? getPackagePlans(oldPackage.getId()) : new ArrayList<>(), enricher);
        return plans;
    }

    protected List<Function> prepareFunctionsStaging(AutomationPackage newPackage, AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent, ObjectEnricher enricher, AutomationPackage oldPackage) {
        List<Function> completeFunctions = new ArrayList<>();
        for (AutomationPackageKeyword keyword : packageContent.getKeywords()) {
            // TODO: here want to apply additional attributes to draft function (upload linked files as resources), but we have to refactor the way to do that
            Function completeFunction = keywordsAttributesApplier.applySpecialAttributesToKeyword(keyword, automationPackageArchive, newPackage.getId(), enricher);
            completeFunctions.add(completeFunction);
        }

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

            String planNameFromSchedule = schedule.getPlanName();
            if (planNameFromSchedule == null || planNameFromSchedule.isEmpty()) {
                throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                        ". Plan name is not defined for schedule " + schedule.getName());
            }

            Plan plan = plansStaging.stream().filter(p -> Objects.equals(p.getAttribute(AbstractOrganizableObject.NAME), planNameFromSchedule)).findFirst().orElse(null);
            if (plan == null) {
                // schedule can reference the existing persisted plan (not defined inside the automation package)
                plan = planAccessor.findByAttributes(Map.of(AbstractOrganizableObject.NAME, planNameFromSchedule));

                if (plan == null) {
                    throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                            ". No plan with '" + planNameFromSchedule + "' name found for schedule " + schedule.getName());
                }
            }
            ExecutionParameters executionParameters = new ExecutionParameters(plan, schedule.getExecutionParameters());
            executionParameters.setRepositoryObject(
                    new RepositoryObjectReference(
                            RepositoryObjectReference.LOCAL_REPOSITORY_ID, Map.of(RepositoryObjectReference.PLAN_ID, plan.getId().toString())
                    )
            );
            execTaskParameters.setExecutionsParameters(executionParameters);
            completeExecTasksParameters.add(execTaskParameters);
        }
        fillEntities(completeExecTasksParameters, oldPackage != null ? getPackageSchedules(oldPackage.getId()) : new ArrayList<>(), enricher);
        return completeExecTasksParameters;
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

    protected AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive) throws AutomationPackageReadingException {
        AutomationPackageContent packageContent;
        packageContent = packageReader.readAutomationPackage(automationPackageArchive, false);
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
                if (executionScheduler != null) {
                    executionScheduler.removeExecutionTask(schedule.getId().toString());
                } else {
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

    public List<Resource> getPackageResources(ObjectId automationPackageId){
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

    private static File stream2file(InputStream in, String fileName) throws IOException {
        final File tempFile = File.createTempFile(fileName, ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }

    public static class PackageUpdateResult {
        private final PackageUpdateStatus status;
        private final ObjectId id;

        public PackageUpdateResult(PackageUpdateStatus status, ObjectId id) {
            this.status = status;
            this.id = id;
        }

        public PackageUpdateStatus getStatus() {
            return status;
        }

        public ObjectId getId() {
            return id;
        }
    }

    public enum PackageUpdateStatus {
        CREATED,
        UPDATED
    }

}
