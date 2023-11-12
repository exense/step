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
import step.attachments.FileResolver;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.model.AutomationPackageSchedule;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;
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
    private final ResourceManager resourceManager;
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
        this.planAccessor = planAccessor;
        this.resourceManager = resourceManager;
        this.executionTaskAccessor = executionTaskAccessor;

        // TODO: avoid using executionScheduler
        this.executionScheduler = executionScheduler;

        // TODO: actual json schema should be resolved from descriptor
        this.packageReader = new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH);
        this.keywordsAttributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);

        // TODO: register new entity via AutomationPackagePlugin (like for FunctionPackagePlugin)
    }

    public AutomationPackage getAutomationPackage(String id) {
        return get(new ObjectId(id));
    }

    public void removeAutomationPackage(String id) {
        remove(new ObjectId(id));
    }

    public String createAutomationPackage(InputStream packageStream, String fileName, ObjectEnricher enricher) {
        try {
            AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(stream2file(packageStream));
            AutomationPackageContent packageContent = packageReader.readAutomationPackage(automationPackageArchive);
            if (packageContent.getName() == null || packageContent.getName().isEmpty()) {
                throw new AutomationPackageManagerException("Automation package name is missing");
            }

            HashMap<String, String> attributes = new HashMap<>();
            attributes.put(AbstractOrganizableObject.NAME, packageContent.getName());
            AutomationPackage existingPackage = automationPackageAccessor.findByAttributes(attributes);
            if (existingPackage != null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' already exists");
            }

            AutomationPackage newPackage = new AutomationPackage();
            newPackage.addAttribute(AbstractOrganizableObject.NAME, packageContent.getName());
            newPackage.addAttribute(AbstractOrganizableObject.VERSION, packageContent.getVersion());

            newPackage.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_FILE_NAME, fileName);

            List<ExecutiontTaskParameters> completeExecTasksParameters = new ArrayList<>();
            for (AutomationPackageSchedule schedule : packageContent.getSchedules()) {
                ExecutiontTaskParameters execTaskParameters = new ExecutiontTaskParameters();
                // TODO: add 'active' field to yaml
                execTaskParameters.setActive(true);
                execTaskParameters.addAttribute(AbstractOrganizableObject.NAME, schedule.getName());
                execTaskParameters.setCronExpression(schedule.getCron());

                Plan plan = packageContent.getPlans().stream().filter(p -> Objects.equals(p.getAttribute(AbstractOrganizableObject.NAME), schedule.getPlanName())).findFirst().orElse(null);
                if (plan == null) {
                    throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                            " No plan with '" + schedule.getCron() + "' name found for schedule " + schedule.getName());
                }
                execTaskParameters.setExecutionsParameters(new ExecutionParameters(plan, schedule.getExecutionParameters()));
                if (enricher != null) {
                    enricher.accept(execTaskParameters);
                }
                completeExecTasksParameters.add(execTaskParameters);
            }

            List<Function> completeFunctions = new ArrayList<>();
            for (AutomationPackageKeyword keyword : packageContent.getKeywords()) {
                // TODO: here want to apply additional attributes to draft function (upload linked files as resources), but we have to refactor the way to do that
                Function completeFunction = keywordsAttributesApplier.applySpecialAttributesToKeyword(keyword, automationPackageArchive);
                completeFunction.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID, newPackage.getId().toString());
                if (enricher != null) {
                    enricher.accept(completeFunction);
                }
                completeFunctions.add(completeFunction);
            }

            List<Plan> completePlans = new ArrayList<>();
            for (Plan plan : packageContent.getPlans()) {
                plan.addCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID, newPackage.getId().toString());
                if (enricher != null) {
                    enricher.accept(plan);
                }
                completePlans.add(plan);
            }

            // persist all staged entities
            for (Function completeFunction : completeFunctions) {
                functionManager.saveFunction(completeFunction);
            }

            for (Plan plan : completePlans) {
                planAccessor.save(plan);
            }

            for (ExecutiontTaskParameters execTasksParameter : completeExecTasksParameters) {
                executionScheduler.addExecutionTask(execTasksParameter);
            }

            String result = automationPackageAccessor.save(newPackage).getId().toString();
            log.info("New automation package saved ({}). Plans: {}. Functions: {}. Schedules: {}", result, completePlans.size(), completeFunctions.size(), completeExecTasksParameters.size());
            return result;

        } catch (Exception e) {
            throw new AutomationPackageManagerException("Unable to read/save automation package", e);
        }
    }

    // TODO: implement the better way to read automation package from input stream
    private static File stream2file(InputStream in) throws IOException {
        final File tempFile = File.createTempFile("autopack", ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }

    private AutomationPackage get(ObjectId id) {
        return automationPackageAccessor.get(id);
    }

    private void remove(ObjectId id) throws AutomationPackageManagerException {
        AutomationPackage automationPackage = automationPackageAccessor.get(id);
        if (automationPackage == null) {
            throw new AutomationPackageManagerException("Automation package not found by id: " + id);
        }

        deleteFunctions(automationPackage);
        deletePlans(automationPackage);

        // TODO: manage tasks carefully
        deleteTasks(automationPackage);

        automationPackageAccessor.remove(id);
    }

    private List<ExecutiontTaskParameters> deleteTasks(AutomationPackage automationPackage) {
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

    private List<Plan> deletePlans(AutomationPackage automationPackage) {
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

    private List<Function> deleteFunctions(AutomationPackage automationPackage) {
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

    private List<Function> getPackageFunctions(AutomationPackage automationPackage) {
        return getFunctionsByCriteria(getAutomationPackageIdCriteria(automationPackage.getId().toString()));
    }

    private static Map<String, String> getAutomationPackageIdCriteria(String automationPackageId) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID, automationPackageId);
        return criteria;
    }

    private List<Plan> getPackagePlans(AutomationPackage automationPackage) {
        return planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(automationPackage.getId().toString())).collect(Collectors.toList());
    }

    private List<ExecutiontTaskParameters> getPackageTasks(AutomationPackage automationPackage) {
        return executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(automationPackage.getId().toString())).collect(Collectors.toList());
    }

}
