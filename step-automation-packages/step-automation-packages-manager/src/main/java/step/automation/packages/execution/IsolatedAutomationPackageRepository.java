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
package step.automation.packages.execution;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageManagerException;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.*;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.resources.*;

import java.io.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class IsolatedAutomationPackageRepository extends AbstractRepository {

    public static final String REPOSITORY_PARAM_CONTEXTID = "contextid";

    public static final Logger log = LoggerFactory.getLogger(IsolatedAutomationPackageRepository.class);
    public static final String CONTEXT_ID_CUSTOM_FIELD = "contextId";
    public static final String AP_NAME_CUSTOM_FIELD = "apName";
    public static final String LAST_EXECUTION_TIME_CUSTOM_FIELD = "lastExecutionTime";
    public static final String PLAN_NAME = "planName";
    public static final String AP_NAME = "apName";

    // context id -> automation package manager (cache)
    private final ConcurrentHashMap<String, PackageExecutionContext> sharedPackageExecutionContexts = new ConcurrentHashMap<>();

    private final AutomationPackageManager manager;
    private final ResourceManager resourceManager;
    private final FunctionTypeRegistry functionTypeRegistry;
    private final FunctionAccessor functionAccessor;
    private final Supplier<String> ttlValueSupplier;

    protected IsolatedAutomationPackageRepository(AutomationPackageManager manager,
                                                  ResourceManager resourceManager,
                                                  FunctionTypeRegistry functionTypeRegistry,
                                                  FunctionAccessor functionAccessor,
                                                  Supplier<String> ttlValueSupplier) {
        super(Set.of(REPOSITORY_PARAM_CONTEXTID));
        this.manager = manager;
        this.resourceManager = resourceManager;
        this.functionTypeRegistry = functionTypeRegistry;
        this.functionAccessor = functionAccessor;
        this.ttlValueSupplier = ttlValueSupplier;
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) {
        // we expect, that there is only one automation package stored per context
        String apName = repositoryParameters.get(AP_NAME);
        String contextId = repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID);

        Resource resource = getResource(contextId, apName);
        if (resource == null) {
            return null;
        }

        ArtefactInfo info = new ArtefactInfo();
        info.setType("automationPackage");
        info.setName(resource.getCustomField(AP_NAME_CUSTOM_FIELD, String.class));
        return info;
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) throws Exception {
        return new TestSetStatusOverview();
    }

    private PackageExecutionContext getOrRestorePackageExecutionContext(Map<String, String> repositoryParameters, ObjectEnricher enricher, ObjectPredicate predicate) {
        String apName = repositoryParameters.get(AP_NAME);
        String contextId = repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID);

        // Execution context can be created in-advance and shared between several plans
        PackageExecutionContext current = sharedPackageExecutionContexts.get(contextId);
        if (current == null) {
            // Here we resolve the original AP file used for previous isolated execution and re-use it to create the execution context
            Resource resource = getResource(contextId, apName);
            if (resource == null) {
                throw new AutomationPackageManagerException("The requested Automation Package file has been removed by the housekeeping (package name '" + apName + "' and execution context " + contextId + ")");
            }

            File apFile = null;

            ResourceRevisionFileHandle fileHandle = resourceManager.getResourceFile(resource.getId().toString());
            if (fileHandle != null) {
                apFile = fileHandle.getResourceFile();
            }
            if (apFile == null) {
                throw new AutomationPackageManagerException("Automation package file is not found for automation package '" + apName + "' and execution context " + contextId);
            }

            updateLastExecution(resource);

            // prepare the isolated in-memory automation package manager with the only one automation package
            AutomationPackageManager inMemoryPackageManager = manager.createIsolated(
                    new ObjectId(contextId), functionTypeRegistry,
                    functionAccessor
            );

            // create single automation package in isolated manager
            try (FileInputStream fis = new FileInputStream(apFile)) {
                inMemoryPackageManager.createAutomationPackage(fis, apFile.getName(), enricher, predicate);
            } catch (IOException e) {
                throw new AutomationPackageManagerException("Cannot read the AP file: " + apFile.getName());
            }

            return new PackageExecutionContext(contextId, inMemoryPackageManager, false);
        }
        return current;
    }

    protected void updateLastExecution(Resource resource) {
        try {
            resource.addCustomField(LAST_EXECUTION_TIME_CUSTOM_FIELD, OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            resourceManager.saveResource(resource);
        } catch (IOException exception) {
            throw new AutomationPackageManagerException("Cannot update the execution time for automation package " + resource.getCustomField(AP_NAME_CUSTOM_FIELD));
        }
    }

    protected Resource getResource(String contextId, String apName) {
        List<Resource> foundResources = resourceManager.findManyByCriteria(
                Map.of("resourceType", ResourceManager.RESOURCE_TYPE_ISOLATED_AP,
                        "customFields." + CONTEXT_ID_CUSTOM_FIELD, contextId,
                        "customFields." + AP_NAME_CUSTOM_FIELD, apName)
        );
        Resource resource = null;
        if (!foundResources.isEmpty()) {
            resource = foundResources.get(0);
        }
        return resource;
    }

    public void cleanUpOutdatedResources() {
        log.info("Cleanup outdated automation packages...");
        String ttlString = ttlValueSupplier.get();

        long ttlDurationMs = Long.parseLong(ttlString);
        Duration ttlDuration = Duration.ofMillis(ttlDurationMs);
        OffsetDateTime minExecutionTime = OffsetDateTime.now().minus(ttlDuration);

        List<Resource> foundResources = resourceManager.findManyByCriteria(
                Map.of("resourceType", ResourceManager.RESOURCE_TYPE_ISOLATED_AP)
        );

        int removed = 0;
        for (Resource foundResource : foundResources) {
            String apResourceInfo = getApResourceInfo(foundResource);
            try {
                String lastExecutionTimeStr = foundResource.getCustomField(LAST_EXECUTION_TIME_CUSTOM_FIELD, String.class);
                if (lastExecutionTimeStr != null) {
                    OffsetDateTime lastExecutionTime = OffsetDateTime.parse(lastExecutionTimeStr, DateTimeFormatter.ISO_DATE_TIME);
                    if (lastExecutionTime.isBefore(minExecutionTime)) {
                        log.info("Cleanup the outdated resource for automation package: {} ...", apResourceInfo);
                        resourceManager.deleteResource(foundResource.getId().toString());
                        removed++;
                    }
                } else {
                    log.warn("The last execution time is unknown for automation package: {}", apResourceInfo);
                }
            } catch (Exception e) {
                log.error("Unable to cleanup outdated resource for automation package: {}", apResourceInfo);
            }
        }
        log.info("Cleanup outdated automation packages finished. {} of {} packages have been removed", removed, foundResources.size());
    }

    private String getApResourceInfo(Resource resource){
        return resource.getCustomField(AP_NAME_CUSTOM_FIELD) + " (ctx=" + resource.getCustomField(CONTEXT_ID_CUSTOM_FIELD) + ")";
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws IOException {
        PackageExecutionContext ctx = null;

        try {
        ImportResult result = new ImportResult();
        try {
            ctx = getOrRestorePackageExecutionContext(repositoryParameters, context.getObjectEnricher(), context.getObjectPredicate());
        } catch (AutomationPackageManagerException e) {
            result.setErrors(List.of(e.getMessage()));
            return result;
        }
        AutomationPackage automationPackage = ctx.getAutomationPackage();

        // PLAN_NAME but not PLAN_ID is used, because plan id is not persisted for isolated execution
        // (it is impossible to re-run the execution by plan id)
        String planName = repositoryParameters.get(PLAN_NAME);

        // validation - the plan should be represented in AP
        AutomationPackageManager apManager = ctx.getInMemoryManager();
        Plan plan = apManager.getPackagePlans(automationPackage.getId())
                .stream()
                .filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(planName)).findFirst().orElse(null);
        if (plan == null) {
            // failed result
            result.setErrors(List.of("Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " has no plan with id=" + planId));
            return result;
        }

        // the plan accessor in context should be layered with 'inMemory' accessor on the top to temporarily store
        // all plans from AP (in code below)
        PlanAccessor planAccessor = context.getPlanAccessor();
        if (!isLayeredAccessor(planAccessor)) {
            result.setErrors(List.of(planAccessor.getClass() + " is not layered"));
            return result;
        }

            // save ALL plans from AP to the execution context to support the 'callPlan' artefact
        // (if some plan from the AP is call from 'callPlan', it should be saved in execution context)
        for (Plan packagePlan : apManager.getPackagePlans(automationPackage.getId())) {
            enrichPlan(context, packagePlan);
            planAccessor.save(packagePlan);
        }

            // populate function accessor for execution context with all functions loaded from AP
        FunctionAccessor functionAccessor = context.get(FunctionAccessor.class);
        List<Function> functionsForSave = new ArrayList<>();
        if (plan.getFunctions() != null) {
            plan.getFunctions().iterator().forEachRemaining(functionsForSave::add);
        }
        functionsForSave.addAll(apManager.getPackageFunctions(automationPackage.getId()));
            functionAccessor.save(functionsForSave);

            ResourceManager contextResourceManager = context.getResourceManager();
            if (!(contextResourceManager instanceof LayeredResourceManager)) {
                result.setErrors(List.of(contextResourceManager.getClass() + " is not layered"));
                return result;
            }

            // import all resources from automation package to execution context by adding the layer to contextResourceManager
            // resource manager used in isolated package manager is non-permanent
            ((LayeredResourceManager) contextResourceManager).pushManager(apManager.getResourceManager(), false);

            // call some hooks on import
            apManager.runExtensionsBeforeIsolatedExecution(automationPackage, context, apManager.getExtensions(), result);

            result.setSuccessful(true);

            return result;
        } finally {
            // if the context is created externally (shared for several plans), it should be managed (closed) in the calling code
            if (ctx != null && !ctx.isExternallyCreatedContext()) {
                ctx.close();
            }
        }
    }

    private static boolean isLayeredAccessor(Accessor<?> accessor) {
        return accessor instanceof LayeredAccessor;
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {

    }

    public PackageExecutionContext createPackageExecutionContext(String contextId, InputStream apStream, String fileName, ObjectEnricher enricher, ObjectPredicate predicate) {
        // prepare the isolated in-memory automation package manager with the only one automation package
        AutomationPackageManager inMemoryPackageManager = manager.createIsolated(
                new ObjectId(contextId), functionTypeRegistry,
                functionAccessor
        );

        // create single automation package in isolated manager
        inMemoryPackageManager.createAutomationPackage(apStream, fileName, enricher, predicate);

        PackageExecutionContext ctx = new PackageExecutionContext(contextId, inMemoryPackageManager, true);
        sharedPackageExecutionContexts.put(contextId, ctx);
        return ctx;
    }

    public Resource saveApResource(String contextId, InputStream apStream, String fileName) {
        // store file in temporary storage to support rerun
        try {
            // find by resource type and contextId (or apName and override)
            ResourceRevisionContainer resourceContainer = resourceManager.createResourceContainer(ResourceManagerImpl.RESOURCE_TYPE_ISOLATED_AP, fileName);

            Resource resource = resourceContainer.getResource();
            resource.addCustomField(CONTEXT_ID_CUSTOM_FIELD, contextId);
            resource.addCustomField(LAST_EXECUTION_TIME_CUSTOM_FIELD, OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            resourceManager.saveResource(resource);

            resource = resourceManager.saveResourceContent(resource.getId().toString(), apStream, fileName);

            return resource;
        } catch (IOException | InvalidResourceFormatException ex) {
            throw new AutomationPackageManagerException("Cannot save automation package as resource: " + fileName, ex);
        }
    }

    public File getApFile(Resource resource) {
        return resourceManager.getResourceFile(resource.getId().toString()).getResourceFile();
    }

    public void setApNameForResource(Resource resource, String apName){
        // store file in temporary storage to support rerun
        try {
            resource.addCustomField(AP_NAME_CUSTOM_FIELD, apName);
            resourceManager.saveResource(resource);
        } catch (IOException ex) {
            throw new AutomationPackageManagerException("Cannot update the automation package name in resource: " + resource.getId(), ex);
        }
    }

    public class PackageExecutionContext implements Closeable {
        private final String contextId;
        private final AutomationPackageManager inMemoryManager;
        private final boolean externallyCreatedContext;

        public PackageExecutionContext(String contextId, AutomationPackageManager inMemoryManager, boolean externallyCreatedContext) {
            this.contextId = contextId;
            this.inMemoryManager = inMemoryManager;
            this.externallyCreatedContext = externallyCreatedContext;
        }

        public AutomationPackageManager getInMemoryManager() {
            return inMemoryManager;
        }

        public AutomationPackage getAutomationPackage() {
            return getInMemoryManager().getAllAutomationPackages(null).findFirst().orElse(null);
        }

        public boolean isExternallyCreatedContext() {
            return externallyCreatedContext;
        }

        @Override
        public void close() throws IOException {

            // only after isolated execution is finished we can clean up temporary created resources
            try {
                // remove the context from isolated automation package repository
                log.info("Cleanup isolated execution context");

                PackageExecutionContext automationPackageManager = sharedPackageExecutionContexts.get(contextId);
                if (automationPackageManager != null) {
                    automationPackageManager.getInMemoryManager().cleanup();
                }
            } finally {
                sharedPackageExecutionContexts.remove(contextId);
            }
        }
    }
}
