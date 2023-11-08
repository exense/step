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
import step.automation.packages.model.AutomationPackage;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.reader.AutomationPackageKeywordsAttributesApplier;
import step.automation.packages.reader.AutomationPackageReader;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutiontTaskParameters;
import step.functions.Function;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeException;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AutomationPackageManager {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageManager.class);

    private final AutomationPackageAccessor automationPackageAccessor;
    private final FunctionManager functionManager;
    private final PlanAccessor planAccessor;
    private final ResourceManager resourceManager;
    private final ExecutionTaskAccessor executionTaskAccessor;
    private final FileResolver fileResolver;
    private final AutomationPackageReader packageReader;
    private final AutomationPackageKeywordsAttributesApplier keywordsAttributesApplier;

    public AutomationPackageManager(AutomationPackageAccessor automationPackageAccessor,
                                    FunctionManager functionManager,
                                    PlanAccessor planAccessor,
                                    ResourceManager resourceManager,
                                    ExecutionTaskAccessor executionTaskAccessor,
                                    FileResolver fileResolver) {
        this.automationPackageAccessor = automationPackageAccessor;
        this.functionManager = functionManager;
        this.planAccessor = planAccessor;
        this.resourceManager = resourceManager;
        this.executionTaskAccessor = executionTaskAccessor;
        this.fileResolver = fileResolver;

        // TODO: actual json schema should be resolved from descriptor
        this.packageReader = new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH);
        this.keywordsAttributesApplier = new AutomationPackageKeywordsAttributesApplier(resourceManager);

        // TODO: register new entity via AutomationPackagePlugin (like for FunctionPackagePlugin)
    }

    public AutomationPackagePersistence getAutomationPackage(String id) {
        return get(new ObjectId(id));
    }

    public void removeAutomationPackage(String id) {
        remove(new ObjectId(id));
    }

    public String createAutomationPackage(InputStream packageStream, String fileName) {
        try {
            AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(stream2file(packageStream));
            AutomationPackage packageContent = packageReader.readAutomationPackage(automationPackageArchive);
            if (packageContent.getName() == null || packageContent.getName().isEmpty()) {
                throw new AutomationPackageManagerException("Automation package name is missing");
            }

            HashMap<String, String> attributes = new HashMap<>();
            attributes.put(AbstractOrganizableObject.NAME, packageContent.getName());
            AutomationPackagePersistence existingPackage = automationPackageAccessor.findByAttributes(attributes);
            if (existingPackage != null) {
                throw new AutomationPackageManagerException("Automation package '" + packageContent.getName() + "' already exists");
            }


            // TODO: enrich nested objects with package id
            AutomationPackagePersistence newPackage = new AutomationPackagePersistence();
            newPackage.addAttribute(AbstractOrganizableObject.NAME, packageContent.getName());
            newPackage.addAttribute(AbstractOrganizableObject.VERSION, packageContent.getVersion());

            Resource createdResource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_AUTOMATION_PACKAGE, packageStream, fileName, false, null);
            newPackage.setPackageLocation(FileResolver.RESOURCE_PREFIX + createdResource.getId().toString());

            List<Function> completeFunctions = new ArrayList<>();
            for (AutomationPackageKeyword keyword : packageContent.getKeywords()) {
                // TODO: here want to apply additional attributes to draft function (upload linked files as resources), but we have to refactor the way to do that
                Function completeFunction = keywordsAttributesApplier.applySpecialAttributesToKeyword(keyword, automationPackageArchive);
                completeFunctions.add(completeFunction);
            }

            for (Function completeFunction : completeFunctions) {
                Function savedFunction = functionManager.saveFunction(completeFunction);
                newPackage.getFunctions().add(savedFunction.getId());
            }

            for (Plan plan : packageContent.getPlans()) {
                Plan savedPlan = planAccessor.save(plan);
                newPackage.getPlans().add(savedPlan.getId());
            }

            // TODO: manage and save execution tasks

            return automationPackageAccessor.save(newPackage).getId().toString();

            // TODO: package descriptor?

        } catch (Exception e) {
            throw new AutomationPackageManagerException("Unable to read/save automation package", e);
        }
    }

    // TODO: implement the better way to read automation package from input stream
    private static File stream2file (InputStream in) throws IOException {
        final File tempFile = File.createTempFile("autopack", ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }

    private AutomationPackagePersistence get(ObjectId id) {
        return automationPackageAccessor.get(id);
    }

    private void remove(ObjectId id) throws AutomationPackageManagerException {
        AutomationPackagePersistence automationPackage = automationPackageAccessor.get(id);
        if (automationPackage == null) {
            throw new AutomationPackageManagerException("Automation package not found by id: " + id);
        }

        deleteFunctions(automationPackage);
        deletePlans(automationPackage);

        // TODO: manage tasks carefully
        deleteTasks(automationPackage);

        //TODO: watchers?
//        unregisterWatcher(automationPackage);

        automationPackageAccessor.remove(id);
        deleteResource(automationPackage.getPackageLocation());
    }

    private List<ExecutiontTaskParameters> deleteTasks(AutomationPackagePersistence automationPackage) {
        List<ExecutiontTaskParameters> tasks = getPackageTasks(automationPackage);
        tasks.forEach(task -> {
            try {
                executionTaskAccessor.remove(task.getId());
            } catch (Exception e) {
                log.error("Error while deleting task " + task.getId().toString(), e);
            }
        });
        return tasks;
    }

    private List<Plan> deletePlans(AutomationPackagePersistence automationPackage) {
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

    private List<Function> deleteFunctions(AutomationPackagePersistence automationPackage) {
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

    private List<Function> getPackageFunctions(AutomationPackagePersistence automationPackagePersistence) {
        return automationPackagePersistence.functions.stream().map(id -> functionManager.getFunctionById(id.toString()))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Plan> getPackagePlans(AutomationPackagePersistence automationPackagePersistence) {
        return automationPackagePersistence.plans.stream().map(id -> planAccessor.get(id.toString()))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<ExecutiontTaskParameters> getPackageTasks(AutomationPackagePersistence automationPackagePersistence) {
        return automationPackagePersistence.tasks.stream().map(id -> executionTaskAccessor.get(id.toString()))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void deleteResource(String path) {
        String resolveResourceId = fileResolver.resolveResourceId(path);

        // Is it a resource?
        if (resolveResourceId != null) {
            // if yes, delete it
            try {
                resourceManager.deleteResource(resolveResourceId);
            } catch (RuntimeException e) {
                log.warn("Dirty cleanup of Automation package: an error occured while deleting one of the associated resources.", e);
            }
        }
    }
}
