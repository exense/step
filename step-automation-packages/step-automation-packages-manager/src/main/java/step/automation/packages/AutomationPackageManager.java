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

import com.google.api.client.util.IOUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.model.AutomationPackageSchedule;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
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
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

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
        this.keywordsAttributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);
    }

    public AutomationPackage getAutomationPackageByName(String name) {
        return automationPackageAccessor.findByAttributes(Map.of(AbstractOrganizableObject.NAME, name));
    }

    public void removeAutomationPackage(String name) {
        AutomationPackage automationPackage = getAutomationPackageByName(name);
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
        deleteTasks(automationPackage);
    }

    public String createAutomationPackage(InputStream packageStream, String fileName, ObjectEnricher enricher) throws FunctionTypeException, SetupFunctionException, AutomationPackageManagerException {
        return createOrUpdateAutomationPackage(false, packageStream, fileName, enricher);
    }

    public String updateAutomationPackage(InputStream packageStream, String fileName, ObjectEnricher enricher) throws SetupFunctionException, FunctionTypeException {
        return createOrUpdateAutomationPackage(true, packageStream, fileName, enricher);
    }

    protected String createOrUpdateAutomationPackage(boolean update, InputStream packageStream, String fileName, ObjectEnricher enricher) throws SetupFunctionException, FunctionTypeException {
        AutomationPackageArchive automationPackageArchive;
        AutomationPackageContent packageContent;
        try {
            automationPackageArchive = new AutomationPackageArchive(stream2file(packageStream));
            packageContent = readAutomationPackage(automationPackageArchive);
        } catch (IOException | AutomationPackageReadingException e) {
            throw new AutomationPackageManagerException("Unable to read automation package", e);
        }

        AutomationPackage oldPackage = getAutomationPackageByName(packageContent.getName());
        if (update) {
            if (oldPackage == null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' doesn't exist");
            }
        } else {
            if (oldPackage != null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' already exists");
            }
        }

        // keep old package id
        AutomationPackage newPackage = createNewInstance(fileName, packageContent, oldPackage);

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
        String result =  automationPackageAccessor.save(newPackage).getId().toString();

        if(oldPackage != null) {
            log.info("Automation package has been updated ({}). Plans: {}. Functions: {}. Schedules: {}", newPackage.getAttribute(AbstractOrganizableObject.NAME), completePlans.size(), completeFunctions.size(), completeExecTasksParameters.size());
        } else {
            log.info("New automation package saved ({}). Plans: {}. Functions: {}. Schedules: {}", newPackage.getAttribute(AbstractOrganizableObject.NAME), completePlans.size(), completeFunctions.size(), completeExecTasksParameters.size());
        }
        return result;
    }

    private void persistStagedEntities(List<ExecutiontTaskParameters> completeExecTasksParameters, List<Function> completeFunctions, List<Plan> completePlans) throws SetupFunctionException, FunctionTypeException {
        for (Function completeFunction : completeFunctions) {
            functionManager.saveFunction(completeFunction);
        }

        for (Plan plan : completePlans) {
            planAccessor.save(plan);
        }

        for (ExecutiontTaskParameters execTasksParameter : completeExecTasksParameters) {
            executionScheduler.addExecutionTask(execTasksParameter, false);
        }

    }

    protected <T extends AbstractOrganizableObject & EnricheableObject> void fillEntities(List<T> entities, AutomationPackage newPackage, List<T> oldEntities, ObjectEnricher enricher){
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
        fillEntities(plans, newPackage, oldPackage != null ? getPackagePlans(oldPackage) : new ArrayList<>(), enricher);
        return plans;
    }

    protected List<Function> prepareFunctionsStaging(AutomationPackage newPackage, AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent, ObjectEnricher enricher, AutomationPackage oldPackage) {
        List<Function> completeFunctions = new ArrayList<>();
        for (AutomationPackageKeyword keyword : packageContent.getKeywords()) {
            // TODO: here want to apply additional attributes to draft function (upload linked files as resources), but we have to refactor the way to do that
            Function completeFunction = keywordsAttributesApplier.applySpecialAttributesToKeyword(keyword, automationPackageArchive);
            completeFunctions.add(completeFunction);
        }

        fillEntities(completeFunctions, newPackage, oldPackage != null ? getPackageFunctions(oldPackage) : new ArrayList<>(), enricher);
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
        fillEntities(completeExecTasksParameters, newPackage, oldPackage != null ? getPackageTasks(oldPackage) : new ArrayList<>(), enricher);
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

    protected AutomationPackage createNewInstance(String fileName, AutomationPackageContent packageContent, AutomationPackage oldPackage) {
        AutomationPackage newPackage = new AutomationPackage();

        // keep old id
        if(oldPackage != null){
            newPackage.setId(oldPackage.getId());
        }
        newPackage.addAttribute(AbstractOrganizableObject.NAME, packageContent.getName());
        newPackage.addAttribute(AbstractOrganizableObject.VERSION, packageContent.getVersion());

        newPackage.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_FILE_NAME, fileName);
        return newPackage;
    }

    protected AutomationPackageContent readAutomationPackage(AutomationPackageArchive automationPackageArchive) throws AutomationPackageReadingException {
        AutomationPackageContent packageContent;
        packageContent = packageReader.readAutomationPackage(automationPackageArchive);
        if (packageContent.getName() == null || packageContent.getName().isEmpty()) {
            throw new AutomationPackageManagerException("Automation package name is missing");
        }
        return packageContent;
    }

    // TODO: find another way to read automation package from input stream
    private static File stream2file(InputStream in) throws IOException {
        final File tempFile = File.createTempFile("autopack", ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }

    protected List<ExecutiontTaskParameters> deleteTasks(AutomationPackage automationPackage) {
        List<ExecutiontTaskParameters> tasks = getPackageTasks(automationPackage);
        tasks.forEach(task -> {
            try {
                executionScheduler.removeExecutionTask(task.getId().toString());
            } catch (Exception e) {
                log.error("Error while deleting task " + task.getId().toString(), e);
            }
        });
        return tasks;
    }

    protected List<Plan> deletePlans(AutomationPackage automationPackage) {
        List<Plan> plans = getPackagePlans(automationPackage);
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
        List<Function> previousFunctions = getPackageFunctions(automationPackage);
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

    protected List<Function> getPackageFunctions(AutomationPackage automationPackage) {
        return getFunctionsByCriteria(getAutomationPackageIdCriteria(automationPackage.getId().toString()));
    }

    protected Map<String, String> getAutomationPackageIdCriteria(String automationPackageId) {
        return Map.of(getAutomationPackageTrackingField(), automationPackageId);
    }

    protected String getAutomationPackageTrackingField() {
        return "customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID;
    }

    protected List<Plan> getPackagePlans(AutomationPackage automationPackage) {
        return planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(automationPackage.getId().toString())).collect(Collectors.toList());
    }

    protected List<ExecutiontTaskParameters> getPackageTasks(AutomationPackage automationPackage) {
        return executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(automationPackage.getId().toString())).collect(Collectors.toList());
    }

}
