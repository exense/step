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

import ch.exense.commons.io.FileHelper;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestCase;
import step.artefacts.TestSet;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageManagerException;
import step.core.execution.ExecutionContext;
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.repositories.ArtefactInfo;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.repositories.ArtifactRepositoryConstants;
import step.resources.*;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class IsolatedAutomationPackageRepository extends RepositoryWithAutomationPackageSupport {

    public static final Logger log = LoggerFactory.getLogger(IsolatedAutomationPackageRepository.class);
    public static final String CONTEXT_ID_CUSTOM_FIELD = "contextId";
    public static final String LAST_EXECUTION_TIME_CUSTOM_FIELD = "lastExecutionTime";

    private final Supplier<String> ttlValueSupplier;
    private final Path mavenCachePath;

    protected IsolatedAutomationPackageRepository(AutomationPackageManager manager,
                                                  ResourceManager resourceManager,
                                                  FunctionTypeRegistry functionTypeRegistry,
                                                  FunctionAccessor functionAccessor,
                                                  Supplier<String> ttlValueSupplier,
                                                  Path mavenCachePath) {
        super(Set.of(REPOSITORY_PARAM_CONTEXTID), manager, functionTypeRegistry, functionAccessor, resourceManager);
        this.ttlValueSupplier = ttlValueSupplier;
        this.mavenCachePath = mavenCachePath;
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) {
        // we expect, that there is only one automation package stored per context
        String apName = repositoryParameters.get(AP_NAME);
        String contextId = repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID);

        Resource resource = getApResource(contextId, apName);
        if (resource == null) {
            return null;
        }

        ArtefactInfo info = new ArtefactInfo();
        boolean isWrapInTestSet = Boolean.parseBoolean(repositoryParameters.getOrDefault(ArtifactRepositoryConstants.PARAM_WRAP_PLANS_INTO_TEST_SET, "false"));
        //Introduced for 28.1, previous execution do not have this field in repo parameters
        String rootType = repositoryParameters.get(ArtifactRepositoryConstants.PARAM_ROOT_TYPE);
        if (rootType != null) {
            info.setType(rootType);
        } else {
            info.setType((isWrapInTestSet) ? TestSet.class.getSimpleName() : TestCase.class.getSimpleName());
        }
        //isolated execution wrapped in TestSet use the AP name as plan name, Non-wrapped AP execution create one execution per plan using the includePlans with the name of the plan to be executed
        info.setName((isWrapInTestSet) ? resource.getCustomField(AP_NAME_CUSTOM_FIELD, String.class) : repositoryParameters.getOrDefault(ArtifactRepositoryConstants.PARAM_INCLUDE_PLANS, apName));
        return info;
    }

    @Override
    public File getArtifact(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) {
        String contextId = repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID);
        if (contextId == null) {
            throw new RuntimeException("Test set status overview cannot be prepared. ContextId is undefined");
        }

        AutomationPackageFile apFile = restorePackageFile(contextId, repositoryParameters, objectPredicate);
        File file = apFile == null ? null : apFile.getFile();
        if (file == null) {
            throw new RuntimeException("Automation package file hasn't been found");
        }
        return file;
    }

    @Override
    public AutomationPackageFile getApFileForExecution(InputStream apInputStream, String inputStreamFileName,
                                                       IsolatedAutomationPackageExecutionParameters parameters, ObjectId contextId,
                                                       ObjectEnricher enricher, ObjectPredicate objectPredicate, String actorUser, String resourceType) {
        // for files from input stream we save persists the resource to support re-execution
        Resource apResource = saveApResource(contextId.toString(), apInputStream, inputStreamFileName, actorUser, resourceType, enricher);
        File file = getApFileByResource(apResource);
        return new AutomationPackageFile(file, apResource);
    }

    public AutomationPackageFile restorePackageFile(String contextId, Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) {
        String apName = repositoryParameters.get(AP_NAME);

        Resource resource = contextId == null ? null : getApResource(contextId, apName);

        if (resource == null) {
            throw new AutomationPackageManagerException("The requested Automation Package file has been removed by the housekeeping (package name '" + apName + "' and execution context " + contextId + ")");
        }

        return getAutomationPackageFileByResource(contextId, resource);
    }

    @Override
    protected boolean isWrapPlansIntoTestSet(Map<String, String> repositoryParameters) {
        return Boolean.parseBoolean(repositoryParameters.getOrDefault(ArtifactRepositoryConstants.PARAM_WRAP_PLANS_INTO_TEST_SET, "false"));
    }

    protected Resource getApResource(String contextId, String apName) {
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

        // find all temporary APs and keyword libs
        List<Resource> foundAps = resourceManager.findManyByCriteria(
                Map.of("resourceType", ResourceManager.RESOURCE_TYPE_ISOLATED_AP)
        );
        List<Resource> foundKeywordLibs = resourceManager.findManyByCriteria(
                Map.of("resourceType", ResourceManager.RESOURCE_TYPE_ISOLATED_AP_LIB)
        );
        List<Resource> foundResources = new ArrayList<>();
        foundResources.addAll(foundAps);
        foundResources.addAll(foundKeywordLibs);

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
        log.info("Cleanup outdated automation packages finished. {} of {} packages have been removed", removed, foundAps.size());
    }

    public void cleanUpMavenCache() {
        log.info("Cleanup outdated artifacts in maven cache...");
        String ttlString = ttlValueSupplier.get();

        long ttlDurationMs = Long.parseLong(ttlString);
        Long minExecutionTime = System.currentTimeMillis() - ttlDurationMs;


        int removed = 0;
        int artifactsCandidate = 0;
        try {
            List<Path> mavenJarFiles = findOldJarFiles(mavenCachePath, minExecutionTime);
            artifactsCandidate = mavenJarFiles.size();
            for(Path jarPath: mavenJarFiles) {
                File parentFile = jarPath.toFile().getParentFile();
                if (parentFile != null) {
                    boolean deleted = FileHelper.safeDeleteFolder(parentFile);;
                    if (deleted) {
                        removed++;
                    } else {
                        log.warn("Unable to cleanup maven cache entry {}, the file might be used", parentFile.getAbsolutePath());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Unable to cleanup local maven cache.", e);
        }

        log.info("Cleanup outdated artifacts in maven cache.. {} of {} artifacts candidates for cleanup have been removed", removed, artifactsCandidate);
    }

    public static List<Path> findOldJarFiles(Path startDir, Long cutoffTime) throws IOException {
        List<Path> oldJarFiles = new ArrayList<>();
        if (startDir.toFile().exists()) {
            Files.walkFileTree(startDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Check if the file is a JAR file
                    if (file.toFile().isFile() && file.toString().endsWith(".jar")) {
                        // Check if the last accessed time is before the cutoff date
                        if (attrs.lastAccessTime().toMillis() < cutoffTime) {
                            oldJarFiles.add(file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return oldJarFiles;
    }

    private String getApResourceInfo(Resource resource){
        return resource.getCustomField(AP_NAME_CUSTOM_FIELD) + " (ctx=" + resource.getCustomField(CONTEXT_ID_CUSTOM_FIELD) + ")";
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {

    }

    private Resource saveApResource(String contextId, InputStream apStream, String fileName, String actorUser, String resourceType, ObjectEnricher enricher) {
        // store file in temporary storage to support rerun
        try {
            // find by resource type and contextId (or apName and override)
            ResourceRevisionContainer resourceContainer = resourceManager.createResourceContainer(resourceType, fileName, actorUser);

            Resource resource = resourceContainer.getResource();
            resource.addCustomField(CONTEXT_ID_CUSTOM_FIELD, contextId);
            resource.addCustomField(LAST_EXECUTION_TIME_CUSTOM_FIELD, OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            enricher.accept(resource);
            resourceManager.saveResource(resource);

            resource = resourceManager.saveResourceContent(resource.getId().toString(), apStream, fileName, null, actorUser);

            return resource;
        } catch (IOException | InvalidResourceFormatException ex) {
            throw new AutomationPackageManagerException("Cannot save automation package as resource: " + fileName + ".", ex, true);
        }
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
            throw new AutomationPackageManagerException("Cannot update the automation package name in resource: " + resource.getId() + ".", ex, true);
        }
    }



}
