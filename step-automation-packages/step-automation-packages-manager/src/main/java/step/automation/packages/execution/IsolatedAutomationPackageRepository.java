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
import step.core.execution.ExecutionContext;
import step.core.execution.model.AutomationPackageExecutionParameters;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.repositories.*;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.resources.*;

import java.io.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class IsolatedAutomationPackageRepository extends RepositoryWithAutomationPackageSupport {

    public static final String REPOSITORY_PARAM_CONTEXTID = "contextid";

    public static final Logger log = LoggerFactory.getLogger(IsolatedAutomationPackageRepository.class);
    public static final String CONTEXT_ID_CUSTOM_FIELD = "contextId";
    public static final String AP_NAME_CUSTOM_FIELD = "apName";
    public static final String LAST_EXECUTION_TIME_CUSTOM_FIELD = "lastExecutionTime";
    public static final String PLAN_NAME = "planName";
    public static final String ORIGINAL_REPOSITORY_ID = "originalRepoId";
    public static final String AP_NAME = "apName";

    private final ResourceManager resourceManager;
    private final RepositoryObjectManager repositoryObjectManager;
    private final Supplier<String> ttlValueSupplier;

    protected IsolatedAutomationPackageRepository(AutomationPackageManager manager,
                                                  ResourceManager resourceManager,
                                                  FunctionTypeRegistry functionTypeRegistry,
                                                  FunctionAccessor functionAccessor,
                                                  RepositoryObjectManager repositoryObjectManager,
                                                  Supplier<String> ttlValueSupplier) {
        super(Set.of(REPOSITORY_PARAM_CONTEXTID), manager, functionTypeRegistry, functionAccessor);
        this.resourceManager = resourceManager;
        this.repositoryObjectManager = repositoryObjectManager;
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
            AutomationPackageFile apFile = restoreApFile(contextId, apName, repositoryParameters);
            return createPackageExecutionContext(enricher, predicate, contextId, apFile);
        }
        return current;
    }

    private AutomationPackageFile restoreApFile(String contextId, String apName, Map<String, String> repositoryParameters) {
        // the file can be stored eiter in some artifact repository (i.e. in Nexus) or in temporary storage for AP files
        String originalRepoId = repositoryParameters.get(ORIGINAL_REPOSITORY_ID);
        if (originalRepoId != null) {
            Repository artifactRepository = repositoryObjectManager.getRepository(originalRepoId);
            File artifact = artifactRepository.getArtifact(repositoryParameters);
            if (artifact == null) {
                throw new AutomationPackageManagerException("Unable to resolve the requested Automation Package file in artifact repository " + originalRepoId + " with parameters " + repositoryParameters);
            }
            return new AutomationPackageFile(artifact, null);
        } else {
            Resource resource = getResource(contextId, apName);

            if (resource == null) {
                throw new AutomationPackageManagerException("The requested Automation Package file has been removed by the housekeeping (package name '" + apName + "' and execution context " + contextId + ")");
            }

            File file = null;

            ResourceRevisionFileHandle fileHandle = resourceManager.getResourceFile(resource.getId().toString());
            if (fileHandle != null) {
                file = fileHandle.getResourceFile();
            }
            if (file == null) {
                throw new AutomationPackageManagerException("Automation package file is not found for automation package '" + apName + "' and execution context " + contextId);
            }

            updateLastExecution(resource);
            return new AutomationPackageFile(file, resource);
        }
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

            AutomationPackageManager apManager = ctx.getInMemoryManager();
            Plan plan = apManager.getPackagePlans(automationPackage.getId())
                    .stream()
                    .filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(planName)).findFirst().orElse(null);
            if (plan == null) {
                // failed result
                result.setErrors(List.of("Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " has no plan with name=" + planName));
                return result;
            }

            return importPlanForIsolatedExecution(context, result, plan, apManager, automationPackage);
        } finally {
            // if the context is created externally (shared for several plans), it should be managed (closed) in the calling code
            closePackageExecutionContext(ctx);
        }
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

    public AutomationPackageFile getApFileForExecution(InputStream apInputStream, String inputStreamFileName, AutomationPackageExecutionParameters parameters, ObjectId contextId) {
        AutomationPackageFile apFile;
        if (apInputStream != null) {
            // for files from input stream we save persists the resource to support re-execution
            Resource apResource = saveApResource(contextId.toString(), apInputStream, inputStreamFileName);
            File file = getApFileByResource(apResource);
            apFile = new AutomationPackageFile(file, apResource);
        } else {
            // for files provided by artifact repository we don't store the file as resource, but just load the file from this repository
            RepositoryObjectReference repositoryObject = parameters.getOriginalRepositoryObject();
            if (repositoryObject == null) {
                throw new AutomationPackageManagerException("Unable to resolve AP file. Repository object is undefined");
            }
            Repository artifactRepository = repositoryObjectManager.getRepository(repositoryObject.getRepositoryID());
            File artifact = artifactRepository.getArtifact(parameters.getOriginalRepositoryObject().getRepositoryParameters());
            return new AutomationPackageFile(artifact, null);
        }
        return apFile;
    }

    private File getApFileByResource(Resource resource) {
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

}
