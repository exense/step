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
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.model.AutomationPackageSchedule;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutiontTaskParameters;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    public void removeAutomationPackage(String name, ObjectPredicate objectPredicate) {
        AutomationPackage automationPackage = getAutomationPackageByName(name, objectPredicate);
        if (automationPackage == null) {
            throw new AutomationPackageManagerException("Automation package not found by name: " + name);
        }

        deleteAutomationPackageEntities(automationPackage);
        automationPackageAccessor.remove(automationPackage.getId());
        log.info("Automation package ({}) has been removed", name);
    }

    private void deleteAutomationPackageEntities(AutomationPackage automationPackage) {
        deleteFunctions(automationPackage);
        deletePlans(automationPackage);
        deleteSchedules(automationPackage);
    }

    public ObjectId createAutomationPackage(InputStream packageStream, String fileName, ObjectEnricher enricher, ObjectPredicate objectPredicate) throws FunctionTypeException, SetupFunctionException, AutomationPackageManagerException {
        return createOrUpdateAutomationPackage(false, packageStream, fileName, enricher, objectPredicate).getId();
    }

    public PackageUpdateResult createOrUpdateAutomationPackage(boolean allowUpdate, InputStream packageStream, String fileName, ObjectEnricher enricher, ObjectPredicate objectPredicate) throws SetupFunctionException, FunctionTypeException {
        AutomationPackageArchive automationPackageArchive;
        AutomationPackageContent packageContent;

        // store automation package
        Resource automationPackageResource = null;
        try {
            automationPackageResource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_AUTOMATION_PACKAGE, packageStream, fileName, false, enricher);
        } catch (Exception ex) {
            throw new AutomationPackageManagerException("Unable to store automation package file");
        }

        try {
            try {
                automationPackageArchive = new AutomationPackageArchive(resourceManager.getResourceFile(automationPackageResource.getId().toString()).getResourceFile());
                packageContent = readAutomationPackage(automationPackageArchive);
            } catch (AutomationPackageReadingException e) {
                throw new AutomationPackageManagerException("Unable to read automation package", e);
            }

            AutomationPackage oldPackage = getAutomationPackageByName(packageContent.getName(), objectPredicate);
            if (!allowUpdate && oldPackage != null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' already exists");
            }

            // keep old package id
            AutomationPackage newPackage = createNewInstance(fileName, packageContent, oldPackage, automationPackageResource, enricher);

            // prepare staging collections
            List<Plan> completePlans = preparePlansStaging(newPackage, packageContent, oldPackage, enricher);
            List<ExecutiontTaskParameters> completeExecTasksParameters = prepareExecutionTasksParamsStaging(newPackage, enricher, packageContent, oldPackage, completePlans);
            List<Function> completeFunctions = prepareFunctionsStaging(newPackage, automationPackageArchive, packageContent, enricher, oldPackage);

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
        } catch (Exception ex){
            // cleanup created resource
            if(automationPackageResource != null){
                resourceManager.deleteResource(automationPackageResource.getId().toString());
            }
            throw ex;
        }
    }

    private void persistStagedEntities(List<ExecutiontTaskParameters> completeExecTasksParameters, List<Function> completeFunctions, List<Plan> completePlans) throws SetupFunctionException, FunctionTypeException {
        for (Function completeFunction : completeFunctions) {
            functionManager.saveFunction(completeFunction);
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

    protected <T extends AbstractOrganizableObject & EnricheableObject> void fillEntities(List<T> entities, AutomationPackage newPackage, List<T> oldEntities, ObjectEnricher enricher) {
        Map<String, ObjectId> planNameToIdMap = createNameToIdMap(oldEntities);

        for (T e : entities) {
            // keep old id
            ObjectId oldId = planNameToIdMap.get(e.getAttribute(AbstractOrganizableObject.NAME));
            if (oldId != null) {
                e.setId(oldId);
            }

            e.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID, newPackage.getId().toString());
            if (enricher != null) {
                enricher.accept(e);
            }
        }
    }

    protected List<Plan> preparePlansStaging(AutomationPackage newPackage, AutomationPackageContent packageContent, AutomationPackage oldPackage, ObjectEnricher enricher) {
        List<Plan> plans = packageContent.getPlans();
        fillEntities(plans, newPackage, oldPackage != null ? getPackagePlans(oldPackage.getId()) : new ArrayList<>(), enricher);
        return plans;
    }

    protected List<Function> prepareFunctionsStaging(AutomationPackage newPackage, AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent, ObjectEnricher enricher, AutomationPackage oldPackage) {
        List<Function> completeFunctions = new ArrayList<>();
        for (AutomationPackageKeyword keyword : packageContent.getKeywords()) {
            // TODO: here want to apply additional attributes to draft function (upload linked files as resources), but we have to refactor the way to do that
            Function completeFunction = keywordsAttributesApplier.applySpecialAttributesToKeyword(keyword, automationPackageArchive, newPackage.getPackageLocation());
            completeFunctions.add(completeFunction);
        }

        fillEntities(completeFunctions, newPackage, oldPackage != null ? getPackageFunctions(oldPackage.getId()) : new ArrayList<>(), enricher);
        return completeFunctions;
    }

    protected List<ExecutiontTaskParameters> prepareExecutionTasksParamsStaging(AutomationPackage newPackage, ObjectEnricher enricher, AutomationPackageContent packageContent, AutomationPackage oldPackage, List<Plan> plansStaging) {
        List<ExecutiontTaskParameters> completeExecTasksParameters = new ArrayList<>();
        for (AutomationPackageSchedule schedule : packageContent.getSchedules()) {
            ExecutiontTaskParameters execTaskParameters = new ExecutiontTaskParameters();

            execTaskParameters.setActive(schedule.getActive() == null || schedule.getActive());
            execTaskParameters.addAttribute(AbstractOrganizableObject.NAME, schedule.getName());
            execTaskParameters.setCronExpression(schedule.getCron());

            Plan plan = plansStaging.stream().filter(p -> Objects.equals(p.getAttribute(AbstractOrganizableObject.NAME), schedule.getPlanName())).findFirst().orElse(null);
            if (plan == null) {
                throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                        " No plan with '" + schedule.getPlanName() + "' name found for schedule " + schedule.getName());
            }
            execTaskParameters.setExecutionsParameters(new ExecutionParameters(plan, schedule.getExecutionParameters()));
            completeExecTasksParameters.add(execTaskParameters);
        }
        fillEntities(completeExecTasksParameters, newPackage, oldPackage != null ? getPackageSchedules(oldPackage.getId()) : new ArrayList<>(), enricher);
        return completeExecTasksParameters;
    }

    private Map<String, ObjectId> createNameToIdMap(List<? extends AbstractOrganizableObject> objects) {
        Map<String, ObjectId> functionNameToIdMap = new HashMap<>();
        for (AbstractOrganizableObject o : objects) {
            String name = o.getAttribute(AbstractOrganizableObject.NAME);
            if (name != null) {
                functionNameToIdMap.put(name, o.getId());
            }
        }
        return functionNameToIdMap;
    }

    protected AutomationPackage createNewInstance(String fileName, AutomationPackageContent packageContent, AutomationPackage oldPackage, Resource automationPackageResource, ObjectEnricher enricher) {
        AutomationPackage newPackage = new AutomationPackage();

        // keep old id
        if (oldPackage != null) {
            newPackage.setId(oldPackage.getId());
        }
        newPackage.addAttribute(AbstractOrganizableObject.NAME, packageContent.getName());
        newPackage.addAttribute(AbstractOrganizableObject.VERSION, packageContent.getVersion());

        newPackage.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_FILE_NAME, fileName);
        newPackage.setPackageLocation(FileResolver.RESOURCE_PREFIX + automationPackageResource);
        if (enricher != null) {
            enricher.accept(newPackage);
        }
        return newPackage;
    }

    protected AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive) throws AutomationPackageReadingException {
        AutomationPackageContent packageContent;
        packageContent = packageReader.readAutomationPackage(automationPackageArchive, false);
        if (packageContent.getName() == null || packageContent.getName().isEmpty()) {
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
                log.error("Error while deleting task " + schedule.getId().toString(), e);
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
                log.error("Error while deleting plan " + plan.getId().toString(), e);
            }
        });
        return plans;
    }

    protected List<Function> deleteFunctions(AutomationPackage automationPackage) {
        List<Function> previousFunctions = getPackageFunctions(automationPackage.getId());
        previousFunctions.forEach(function -> {
            try {
                functionManager.deleteFunction(function.getId().toString());
            } catch (FunctionTypeException e) {
                log.error("Error while deleting function " + function.getId().toString(), e);
            }
        });
        return previousFunctions;
    }

    protected List<Function> getFunctionsByCriteria(Map<String, String> criteria) {
        return functionAccessor.findManyByCriteria(criteria).collect(Collectors.toList());
    }

    public List<Function> getPackageFunctions(ObjectId automationPackageId) {
        return getFunctionsByCriteria(getAutomationPackageIdCriteria(automationPackageId));
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
