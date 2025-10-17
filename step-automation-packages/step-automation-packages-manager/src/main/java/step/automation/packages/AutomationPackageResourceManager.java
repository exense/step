/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.automation.packages;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.library.AutomationPackageLibraryProvider;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.resources.InvalidResourceFormatException;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AutomationPackageResourceManager {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageResourceManager.class);
    private final ResourceManager resourceManager;
    private final AutomationPackageOperationMode operationMode;
    private final AutomationPackageAccessor automationPackageAccessor;
    private final LinkedAutomationPackagesFinder linkedAutomationPackagesFinder;
    private final AutomationPackageMavenConfig mavenConfig;

    public AutomationPackageResourceManager(ResourceManager resourceManager,
                                            AutomationPackageOperationMode operationMode,
                                            AutomationPackageAccessor automationPackageAccessor,
                                            LinkedAutomationPackagesFinder linkedAutomationPackagesFinder,
                                            AutomationPackageMavenConfig mavenConfig) {
        this.resourceManager = resourceManager;
        this.operationMode = operationMode;
        this.automationPackageAccessor = automationPackageAccessor;
        this.linkedAutomationPackagesFinder = linkedAutomationPackagesFinder;
        this.mavenConfig = mavenConfig;
    }

    public Resource uploadOrReuseAutomationPackageLibrary(AutomationPackageLibraryProvider apLibProvider,
                                                        AutomationPackage automationPackageToBeLinkedWithLib,
                                                        ObjectEnricher enricher,
                                                        ObjectPredicate objectPredicate,
                                                        String actorUser, ObjectPredicate accessChecker, boolean allowToReuseOldResource) {
        String apName = automationPackageToBeLinkedWithLib == null ? "" : automationPackageToBeLinkedWithLib.getAttribute(AbstractOrganizableObject.NAME);
        String apLibraryResourceString = null;
        Resource uploadedResource = null;
        try {
            File apLibrary = apLibProvider.getAutomationPackageLibrary();
            if (apLibrary != null) {
                try (FileInputStream fis = new FileInputStream(apLibrary)) {
                    // for isolated execution we always use the isolatedAp resource type to support auto cleanup after execution
                    String resourceType = this.operationMode == AutomationPackageOperationMode.ISOLATED ? ResourceManager.RESOURCE_TYPE_ISOLATED_AP_LIB : apLibProvider.getResourceType();

                    // we can reuse the existing old resource in case it is identifiable (can be found by origin) and unmodifiable
                    List<Resource> oldResources = null;
                    if (apLibProvider.canLookupResources()) {
                        oldResources = apLibProvider.lookupExistingResources(resourceManager, objectPredicate);
                    }

                    if (oldResources != null && !oldResources.isEmpty()) {
                        Resource oldResource = oldResources.get(0);

                        if(!allowToReuseOldResource){
                            throw new AutomationPackageManagerException("Old resource " + oldResource.getResourceName() + " ( " + oldResource.getId() + " ) has been detected and cannot be reused");
                        }

                        if (!apLibProvider.isModifiableResource()) {
                            // for unmodifiable origins we just reused the previously uploaded resource
                            log.info("Existing automation package library {} with resource id {} has been detected and will be reused in AP {}", apLibrary.getName(), oldResource.getId().toHexString(), apName);
                            uploadedResource = oldResource;
                        } else if (apLibProvider.hasNewContent()){
                            // for modifiable resources (i.e. SNAPSHOTS) we can reuse the old resource id and metadata, but we need to update the content if a new version was downloaded
                            uploadedResource = updateExistingResourceContentAndPropagate(apLibrary.getName(),
                                    apName,
                                    automationPackageToBeLinkedWithLib == null ? null : automationPackageToBeLinkedWithLib.getId(),
                                    fis, apLibProvider.getSnapshotTimestamp(), oldResource,
                                    actorUser, accessChecker
                            );
                        } else {
                            uploadedResource = oldResource;
                        }
                    } else {
                        ResourceOrigin origin = apLibProvider.getOrigin();

                        // old resource is not found - we create a new one
                        uploadedResource = resourceManager.createTrackedResource(
                                resourceType, false, fis, apLibrary.getName(), enricher, null,
                                actorUser, origin == null ? null : origin.toStringRepresentation(),
                                apLibProvider.getSnapshotTimestamp()
                        );
                        log.info("The new automation package library ({}) has been uploaded as ({})", apLibProvider, uploadedResource.getId().toHexString());
                    }
                    if(automationPackageToBeLinkedWithLib != null) {
                        apLibraryResourceString = FileResolver.RESOURCE_PREFIX + uploadedResource.getId().toString();
                        automationPackageToBeLinkedWithLib.setAutomationPackageLibraryResource(apLibraryResourceString);
                    }
                }
            }
        } catch (IOException | InvalidResourceFormatException | AutomationPackageReadingException e) {
            // all these exceptions are technical, so we log the whole stack trace here, but throw the AutomationPackageManagerException
            // to provide the short error message without technical details to the client
            log.error("Unable to upload the automation package library", e);
            throw new AutomationPackageManagerException("Unable to upload the automation package library: " + apLibProvider, e);
        }
        return uploadedResource;
    }

    public Resource uploadOrReuseApResource(AutomationPackageArchiveProvider apProvider,
                                            AutomationPackageArchive automationPackageArchive,
                                            AutomationPackage automationPackageToBeLinkedWithResource,
                                            ObjectEnricher enricher, String actorUser,
                                            ObjectPredicate objectPredicate,
                                            ObjectPredicate writeAccessPredicate,
                                            boolean allowToReuseOldResource) {
        ResourceOrigin apOrigin = apProvider.getOrigin();
        File originalFile = automationPackageArchive.getOriginalFile();
        if (originalFile == null) {
            return null;
        }

        Resource resource = null;

        List<Resource> existingResource = null;
        if (apProvider.canLookupResources()) {
            existingResource = apProvider.lookupExistingResources(resourceManager, objectPredicate);
        }

        String apName = automationPackageToBeLinkedWithResource == null ? "" : automationPackageToBeLinkedWithResource.getAttribute(AbstractOrganizableObject.NAME);
        if (existingResource != null && !existingResource.isEmpty()) {
            resource = existingResource.get(0);

            if (!allowToReuseOldResource) {
                throw new AutomationPackageManagerException("Old resource " + resource.getResourceName() + " ( " + resource.getId() + " ) has been detected and cannot be reused");
            }

            // we just reuse the existing resource of unmodifiable origin (i.e non-SNAPSHOT)
            // and for SNAPSHOT we keep the same resource id, but update the content if a new version was found
            if (apProvider.isModifiableResource() && apProvider.hasNewContent()) {
                try (FileInputStream is = new FileInputStream(originalFile)) {
                    resource = updateExistingResourceContentAndPropagate(
                            originalFile.getName(),
                            apName, automationPackageToBeLinkedWithResource == null ? null : automationPackageToBeLinkedWithResource.getId(),
                            is, apProvider.getSnapshotTimestamp(), resource,
                            actorUser, writeAccessPredicate
                    );
                } catch (IOException | InvalidResourceFormatException e) {
                    throw new RuntimeException("Unable to create the resource for automation package", e);
                }
            }
        }

        // create the new resource if the old one cannot be reused
        if (resource == null) {
            try (InputStream is = new FileInputStream(originalFile)) {
                resource = resourceManager.createTrackedResource(
                        ResourceManager.RESOURCE_TYPE_AP, false, is, originalFile.getName(), enricher, null, actorUser,
                        apOrigin == null ? null : apOrigin.toStringRepresentation(), apProvider.getSnapshotTimestamp()
                );
            } catch (IOException | InvalidResourceFormatException e) {
                throw new RuntimeException("Unable to create the resource for automation package", e);
            }
        }

        if (automationPackageToBeLinkedWithResource != null) {
            String resourceString = FileResolver.RESOURCE_PREFIX + resource.getId().toString();
            log.info("The resource has been been linked with AP '{}': {}", apName, resourceString);
            automationPackageToBeLinkedWithResource.setAutomationPackageResource(resourceString);
        }
        return resource;
    }

    /**
     * This method is called for modifiable resource (currently only maven snapshot artefact) to update the existing Step
     * resource with the new content and propagate the update to Automation Packages using this resource
     *
     * @param resourceFileName the resource file name
     * @param apName the name of the automation pacakge
     * @param currentApId the automation package Id
     * @param fis the resource input stream
     * @param newOriginTimestamp the artefact snapshot timestamp (null if no new snapshot was downloaded
     * @param oldResource the resource to be updated if required
     * @param actorUser the user triggering this update
     * @param writeAccessPredicate predicate to check if this resource can be updated in this context
     * @return the updated resource
     * @throws InvalidResourceFormatException in case the new resource content is invalid
     * @throws AutomationPackageAccessException in case the resource of linked AP cannot be updated in the current context
     */
    private Resource updateExistingResourceContentAndPropagate(String resourceFileName,
                                                               String apName, ObjectId currentApId,
                                                               FileInputStream fis, Long newOriginTimestamp,
                                                               Resource oldResource, String actorUser,
                                                               ObjectPredicate writeAccessPredicate) throws IOException, InvalidResourceFormatException, AutomationPackageAccessException {
        String resourceId = oldResource.getId().toHexString();
        //Check write access to the resource itself
        if (!writeAccessPredicate.test(oldResource)) {
            String errorMessage = "The existing resource " + resourceId + " for file " + resourceFileName + " referenced by the provided package cannot be modified in the current context.";
            log.error(errorMessage);
            throw new AutomationPackageAccessException(errorMessage);
        }
        // Check write access to other APs using this resource. We cannot reupload the resources if they are linked with another package, which is not accessible
        List<ObjectId> ignoredApsToLookup = currentApId == null ? List.of() : List.of(currentApId);
        for (ObjectId apId : linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(resourceId, ignoredApsToLookup)) {
            checkAccess(automationPackageAccessor.get(apId), writeAccessPredicate);
        }

        log.info("Existing resource {} for file {} will be actualized and reused in AP {}", resourceId, resourceFileName, apName);
        Resource uploadedResource = resourceManager.saveResourceContent(resourceId, fis, resourceFileName, actorUser);
        uploadedResource.setOriginTimestamp(newOriginTimestamp);
        resourceManager.saveResource(uploadedResource);
        return uploadedResource;
    }

    protected void checkAccess(AutomationPackage automationPackage, ObjectPredicate writeAccessPredicate) throws AutomationPackageAccessException {
        if (writeAccessPredicate != null) {
            if (!writeAccessPredicate.test(automationPackage)) {
                throw new AutomationPackageAccessException(
                        automationPackage,
                        "You're not allowed to edit the linked automation package " + getLogRepresentation(automationPackage) + " from within this context"
                );
            }
        }
    }

    protected void checkAccess(Resource resource, ObjectPredicate writeAccessPredicate) throws AutomationPackageAccessException {
        if (writeAccessPredicate != null) {
            if (!writeAccessPredicate.test(resource)) {
                throw new AutomationPackageAccessException(
                        "You're not allowed to edit the linked automation package " + getLogRepresentation(resource) + " from within this context"
                );
            }
        }
    }

    public RefreshResourceResult refreshResourceAndLinkedPackages(String resourceId,
                                                                  ObjectEnricher objectEnricher,
                                                                  ObjectPredicate objectPredicate,
                                                                  ObjectPredicate writeAccessPredicate,
                                                                  String actorUser,
                                                                  AutomationPackageManager apManager) {
        Resource resource = resourceManager.getResource(resourceId);
        return refreshResourceAndLinkedPackages(resource, writeAccessPredicate, (linkedAutomationPackages, refreshResourceResult) -> {
            List<AutomationPackage> reuploadedPackages = new ArrayList<>(linkedAutomationPackages);
            List<AutomationPackage> failedPackages = new ArrayList<>();
            try {
                apManager.redeployRelatedAutomationPackages(
                        linkedAutomationPackages.stream().map(AbstractIdentifiableObject::getId).collect(Collectors.toList()),
                        objectEnricher, objectPredicate, writeAccessPredicate, actorUser
                );
            } catch (AutomationPackageRedeployException ex) {
                for (ObjectId failedId : ex.getFailedApsId()) {
                    AutomationPackage failedPackage = linkedAutomationPackages.stream().filter(ap -> ap.getId().equals(failedId)).findFirst().orElse(null);
                    if (failedPackage != null) {
                        reuploadedPackages.remove(failedPackage);
                        failedPackages.add(failedPackage);
                    }
                }
            }
            if (!reuploadedPackages.isEmpty()) {
                refreshResourceResult.addInfo("The following automation packages have been reuploaded: " + reuploadedPackages.stream().map(AutomationPackageResourceManager::getLogRepresentation));
            }
            if (!failedPackages.isEmpty()) {
                refreshResourceResult.addInfo("Failed to reupload the following automation packages: " + failedPackages.stream().map(AutomationPackageResourceManager::getLogRepresentation));
            }
        });
    }

    public RefreshResourceResult refreshResourceAndLinkedPackages(Resource resource,
                                                                  ObjectPredicate writeAccessPredicate,
                                                                  LinkedPackagesReuploader linkedPackagesReuploader) {
        RefreshResourceResult refreshResourceResult = new RefreshResourceResult();

        if (!writeAccessPredicate.test(resource)) {
            refreshResourceResult.addError("You have no access to resource with id: " + resource.getId());
            return refreshResourceResult;
        }

        Set<String> supportedResourceTypesForRefresh = Set.of(
                ResourceManager.RESOURCE_TYPE_AP,
                ResourceManager.RESOURCE_TYPE_AP_LIBRARY,
                ResourceManager.RESOURCE_TYPE_ISOLATED_AP,
                ResourceManager.RESOURCE_TYPE_ISOLATED_AP_LIB
        );
        if (!supportedResourceTypesForRefresh.contains(resource.getResourceType())) {
            refreshResourceResult.addError("Unsupported resource type for refresh: " + resource.getResourceType());
        }
        if (!MavenArtifactIdentifier.isMvnIdentifierShortString(resource.getOrigin())) {
            refreshResourceResult.addError("Unsupported resource origin for refresh: " + resource.getOrigin());
        }

        // check access for linked automation packages
        Set<AutomationPackage> linkedAutomationPackages
                = linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(resource.getId().toHexString(), new ArrayList<>())
                .stream()
                .map(automationPackageAccessor::get)
                .collect(Collectors.toSet());

        for (AutomationPackage linkedAutomationPackage : linkedAutomationPackages) {
            try {
                checkAccess(linkedAutomationPackage, writeAccessPredicate);
            } catch (AutomationPackageAccessException ex){
                refreshResourceResult.addError(ex.getMessage());
            }
        }

        if (!refreshResourceResult.isOk()) {
            return refreshResourceResult;
        }

        MavenArtifactIdentifier mavenArtifactIdentifier = MavenArtifactIdentifier.fromShortString(resource.getOrigin());
        if (!mavenArtifactIdentifier.isSnapshot()) {
            log.warn("The maven artifact {} cannot be reloaded, because it is SNAPSHOT", mavenArtifactIdentifier.toStringRepresentation());
        } else {
            // REUPLOAD THE RESOURCE
            try {
                // restore the automation package file from maven
                File file = MavenArtifactDownloader.getFile(mavenConfig, mavenArtifactIdentifier, null).artifactFile;
                try (FileInputStream fis = new FileInputStream(file)) {
                    resourceManager.saveResourceContent(resource.getId().toHexString(), fis, file.getName(), resource.getCreationUser());
                }
            } catch (InvalidResourceFormatException | IOException | AutomationPackageReadingException ex) {
                throw new AutomationPackageManagerException("Cannot restore the file for from maven artifactory", ex);
            }

            // REFRESH LINKED PACKAGES
            if (linkedPackagesReuploader != null) {
                linkedPackagesReuploader.reupload(linkedAutomationPackages, refreshResourceResult);
            }
        }

        return refreshResourceResult;
    }

    public void deleteResource(String resourceId, ObjectPredicate writeAccessPredicate) throws AutomationPackageAccessException {
        Resource resource = resourceManager.getResource(resourceId);
        if (resource == null) {
            throw new AutomationPackageManagerException("Resource is not found by id: " + resourceId);
        }
        checkAccess(resource, writeAccessPredicate);

        Set<ObjectId> linkedAps = linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(resourceId, List.of());

        if (!linkedAps.isEmpty()) {
            throw new AutomationPackageAccessException("Resource " + getLogRepresentation(resource) +
                    " cannot be deleted, because there are automation packages using this resource: " +
                    linkedAps.stream().map(p -> getLogRepresentation(automationPackageAccessor.get(p))).collect(Collectors.toList())
            );
        }
    }

    public List<AutomationPackage> findAutomationPackagesByResourceId(String resourceId, List<ObjectId> ignoredApIds) {
        return linkedAutomationPackagesFinder.findAutomationPackagesByResourceId(resourceId, ignoredApIds);
    }

    private static String getLogRepresentation(AutomationPackage p) {
        return "'" + p.getAttribute(AbstractOrganizableObject.NAME) + "'(" + p.getId() + ")";
    }

    private static String getLogRepresentation(Resource r) {
        return "'" + r.getResourceName() + "'(" + r.getId() + ")";
    }

    public interface LinkedPackagesReuploader {
        void reupload(Set<AutomationPackage> linkedAutomationPackages,
                      RefreshResourceResult refreshResourceResult
        );
    }

}
