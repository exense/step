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
import step.automation.packages.library.ManagedLibraryProvider;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.ObjectAccessException;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.WriteAccessValidator;
import step.repositories.artifact.ResolvedMavenArtifact;
import step.repositories.artifact.SnapshotMetadata;
import step.resources.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class AutomationPackageResourceManager {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageResourceManager.class);
    public static final String MANUALLY_CREATED_AP_RESOURCE = "MANUALLY_CREATED_AP_RESOURCE";
    private final ResourceManager resourceManager;
    private final AutomationPackageOperationMode operationMode;
    private final AutomationPackageAccessor automationPackageAccessor;
    private final LinkedAutomationPackagesFinder linkedAutomationPackagesFinder;
    private final MavenOperations mavenOperations;

    private Set<String> supportedResourceTypes = Set.of(
            ResourceManager.RESOURCE_TYPE_AP,
            ResourceManager.RESOURCE_TYPE_AP_LIBRARY,
            ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY
    );

    public AutomationPackageResourceManager(ResourceManager resourceManager,
                                            AutomationPackageOperationMode operationMode,
                                            AutomationPackageAccessor automationPackageAccessor,
                                            LinkedAutomationPackagesFinder linkedAutomationPackagesFinder,
                                            MavenOperations mavenOperations) {
        this.resourceManager = resourceManager;
        this.operationMode = operationMode;
        this.automationPackageAccessor = automationPackageAccessor;
        this.linkedAutomationPackagesFinder = linkedAutomationPackagesFinder;
        this.mavenOperations = mavenOperations;
    }

    public Resource findManagedLibrary(String managedLibraryName, ObjectPredicate predicate) throws ManagedLibraryMissingException {
        return ManagedLibraryProvider.getManagedLibraryResource(resourceManager, managedLibraryName, predicate);
    }

    /**
     * This method is used to check that we could reuse and update the existing resource, to be called before updating the package resource
     * @param apLibProvider the apLibProvider
     * @param parameters the updated parameters
     * @param allowToReuseOldResource the flag allowing or not to reuse libraries
     */
    public void validateUploadOrReuseAutomationPackageLibrary(AutomationPackageLibraryProvider apLibProvider,
                                                              AutomationPackageUpdateParameter parameters, boolean allowToReuseOldResource) {
        try {
            File apLibrary = apLibProvider.getAutomationPackageLibrary();
            if (apLibrary != null) {
                // we can reuse the existing old resource in case it is identifiable (can be found by origin) and unmodifiable
                if (apLibProvider.canLookupResources()) {
                    Optional<Resource> oldResources = apLibProvider.lookupExistingResource(resourceManager, parameters.objectPredicate);
                    if (oldResources.isPresent()) {
                        //
                        Resource oldResource = oldResources.get();
                        if(!allowToReuseOldResource){
                            throw new AutomationPackageManagerException("Existing library " + oldResource.getResourceName() + " ( " + oldResource.getId() + " ) has been detected and cannot be reused / updated");
                        } else if (apLibProvider.isModifiableResource() && apLibProvider.hasNewContent() && parameters.forceRefreshOfSnapshots) {
                            checkResourceWriteAccess(apLibrary.getName(), oldResource, parameters.writeAccessValidator);
                        }
                    }
                }

            }
        } catch (AutomationPackageReadingException e) {
            // all these exceptions are technical, so we log the whole stack trace here, but throw the AutomationPackageManagerException
            // to provide the short error message without technical details to the client
            log.error("Unable to upload the automation package library", e);
            throw new AutomationPackageManagerException("Unable to upload the automation package library: " + apLibProvider, e);
        }

    }

    public Resource uploadOrReuseAutomationPackageLibrary(AutomationPackageLibraryProvider apLibProvider,
                                                          AutomationPackage automationPackageToBeLinkedWithLib,
                                                          AutomationPackageAccessParameters parameters, boolean allowToReuseOldResource, boolean allowUpdateContent) {
        String apName = automationPackageToBeLinkedWithLib == null ? "" : automationPackageToBeLinkedWithLib.getAttribute(AbstractOrganizableObject.NAME);
        String origin = Optional.ofNullable(apLibProvider.getOrigin()).map(ResourceOrigin::toStringRepresentation).orElse(null);
        String apLibraryResourceString = null;
        Resource uploadedResource = null;
        try {
            File apLibrary = apLibProvider.getAutomationPackageLibrary();
            if (apLibrary != null) {
                // for isolated execution we always use the isolatedAp resource type to support auto cleanup after execution
                String resourceType = this.operationMode == AutomationPackageOperationMode.ISOLATED ? ResourceManager.RESOURCE_TYPE_ISOLATED_AP_LIB : apLibProvider.getResourceType();

                // we can reuse the existing old resource in case it is identifiable (can be found by origin) and unmodifiable
                Optional<Resource> oldResourceOpt = Optional.empty();
                if (apLibProvider.canLookupResources()) {
                    oldResourceOpt = apLibProvider.lookupExistingResource(resourceManager, parameters.objectPredicate);
                }

                if (oldResourceOpt.isPresent()) {
                    Resource oldResource = oldResourceOpt.get();

                    if(!allowToReuseOldResource){
                        throw new AutomationPackageManagerException("Existing resource " + oldResource.getResourceName() + " ( " + oldResource.getId() + " ) has been detected and cannot be reused / updated");
                    }
                    uploadedResource = oldResource;

                    ResourceRevisionFileHandle fileHandle = resourceManager.getResourceFile(oldResource.getId().toString());
                    boolean resourceFileExists = fileHandle != null && fileHandle.getResourceFile() != null && fileHandle.getResourceFile().exists();
                    boolean mavenResourceFileDeleted = !resourceFileExists && MavenArtifactIdentifier.isMvnIdentifierShortString(oldResource.getOrigin());
                    boolean mavenSnapshotWithNewContent = apLibProvider.isModifiableResource() && apLibProvider.hasNewContent() && allowUpdateContent;
                    if (mavenResourceFileDeleted || mavenSnapshotWithNewContent) {
                        // for modifiable resources (i.e. SNAPSHOTS) we can reuse the old resource id and metadata, but we need to update the content if a new version was downloaded
                        uploadedResource = updateExistingResourceContentAndPropagate(parameters, apLibrary, apLibrary.getName(),
                                apName, automationPackageToBeLinkedWithLib == null ? null : automationPackageToBeLinkedWithLib.getId(),
                                origin, apLibProvider.getSnapshotTimestamp(), oldResource, apLibProvider.getResourceName()
                        );

                    }
                } else {
                    // old resource is not found - we create a new one
                    try (FileInputStream fis = new FileInputStream(apLibrary)) {
                        uploadedResource = resourceManager.createTrackedResource(
                                resourceType, false, fis, apLibrary.getName(), apLibProvider.getResourceName(), parameters.enricher, null,
                                parameters.actorUser, origin,
                                apLibProvider.getSnapshotTimestamp()
                        );
                        if (automationPackageToBeLinkedWithLib == null) {
                            uploadedResource.addCustomField(MANUALLY_CREATED_AP_RESOURCE, true);
                            resourceManager.saveResource(uploadedResource);
                        }
                        log.info("The new automation package library ({}) has been uploaded as ({})", apLibProvider, uploadedResource.getId().toHexString());
                    }
                }
                if(automationPackageToBeLinkedWithLib != null) {
                    apLibraryResourceString = FileResolver.createPathForResource(uploadedResource);
                    automationPackageToBeLinkedWithLib.setAutomationPackageLibraryResource(apLibraryResourceString);
                    automationPackageToBeLinkedWithLib.setAutomationPackageLibraryResourceRevision(FileResolver.createRevisionPathForResource(uploadedResource));
                }
            }
        } catch (IOException | InvalidResourceFormatException | AutomationPackageReadingException |
                 AutomationPackageUnsupportedResourceTypeException e) {
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
                                            AutomationPackageAccessParameters parameters,
                                            boolean allowToReuseOldResource, boolean allowUpdateContent) {
        String origin = Optional.ofNullable(apProvider.getOrigin()).map(ResourceOrigin::toStringRepresentation).orElse(null);
        File originalFile = automationPackageArchive.getOriginalFile();
        if (originalFile == null) {
            return null;
        }

        Resource resource = null;

        Optional<Resource> existingResource = Optional.empty();
        if (apProvider.canLookupResources()) {
            existingResource = apProvider.lookupExistingResource(resourceManager, parameters.objectPredicate);
        }

        String apName = automationPackageToBeLinkedWithResource == null ? "" : automationPackageToBeLinkedWithResource.getAttribute(AbstractOrganizableObject.NAME);
        if (existingResource.isPresent()) {
            resource = existingResource.get();

            if (!allowToReuseOldResource) {
                throw new AutomationPackageManagerException("Existing resource " + resource.getResourceName() + " ( " + resource.getId() + " ) has been detected and cannot be reused / updated");
            }

            ResourceRevisionFileHandle fileHandle = resourceManager.getResourceFile(resource.getId().toString());
            boolean resourceFileExists = fileHandle != null && fileHandle.getResourceFile() != null && fileHandle.getResourceFile().exists();

            //For existing maven artefact resources, we get new content if the resource file doesn't exist anymore or it is a
            // snapshot with new content
            boolean mavenResourceFileDeleted = !resourceFileExists && MavenArtifactIdentifier.isMvnIdentifierShortString(resource.getOrigin());
            boolean mavenSnapshotWithNewContent = apProvider.isModifiableResource() && apProvider.hasNewContent() && allowUpdateContent;
            if (mavenResourceFileDeleted || mavenSnapshotWithNewContent) {
                try {
                    resource = updateExistingResourceContentAndPropagate(parameters, originalFile,
                            originalFile.getName(), apName,
                            Optional.ofNullable(automationPackageToBeLinkedWithResource).map(AbstractIdentifiableObject::getId).orElse(null),
                            origin, apProvider.getSnapshotTimestamp(), resource, apProvider.getResourceName()
                    );
                } catch (IOException | InvalidResourceFormatException |
                         AutomationPackageUnsupportedResourceTypeException e) {
                    throw new AutomationPackageManagerException("Unable to create the resource for automation package", e);
                }
            }
        }

        // create the new resource if the old one cannot be reused
        if (resource == null) {
            try (InputStream is = new FileInputStream(originalFile)) {
                resource = resourceManager.createTrackedResource(
                        ResourceManager.RESOURCE_TYPE_AP, false, is, originalFile.getName(), apProvider.getResourceName(), parameters.enricher, null, parameters.actorUser,
                        origin, apProvider.getSnapshotTimestamp()
                );
            } catch (IOException | InvalidResourceFormatException e) {
                throw new RuntimeException("Unable to create the resource for automation package", e);
            }
        }

        if (automationPackageToBeLinkedWithResource != null) {
            String resourceString = FileResolver.createPathForResource(resource);
            automationPackageToBeLinkedWithResource.setAutomationPackageResource(resourceString);
            automationPackageToBeLinkedWithResource.setAutomationPackageResourceRevision(FileResolver.createRevisionPathForResource(resource));
            log.info("The resource has been been linked with AP '{}': {}", apName, resourceString);
        }
        return resource;
    }

    /**
     * This method is called for modifiable resource (currently only maven snapshot artefact) to update the existing Step
     * resource with the new content and propagate the update to Automation Packages using this resource
     *
     * @param parameters contains access context parameters (enrichor, actinguser, access validators...)
     * @param originalFile the source file
     * @param resourceFileName the resource file name
     * @param apName the name of the automation package
     * @param currentApId the automation package Id
     * @param origin origin of the package or library
     * @param newOriginTimestamp the artefact snapshot timestamp (null if no new snapshot was downloaded
     * @param oldResource the resource to be updated if required

     * @return the updated resource
     * @throws InvalidResourceFormatException in case the new resource content is invalid
     * @throws AutomationPackageAccessException in case the resource of linked AP cannot be updated in the current context
     */
    private Resource updateExistingResourceContentAndPropagate(AutomationPackageAccessParameters parameters,
                                                               File originalFile,
                                                               String resourceFileName,
                                                               String apName,
                                                               ObjectId currentApId,
                                                               String origin,
                                                               Long newOriginTimestamp,
                                                               Resource oldResource,
                                                               String newResourceName) throws IOException, InvalidResourceFormatException, AutomationPackageAccessException, AutomationPackageUnsupportedResourceTypeException {
        try (FileInputStream fis = new FileInputStream(originalFile)) {
            String resourceId = oldResource.getId().toHexString();
            //Check write access to the resource itself
            checkResourceWriteAccess(resourceFileName, oldResource, parameters.writeAccessValidator);
            validateResourceType(oldResource);

            log.info("Existing resource {} for file {} will be actualized and reused in AP {}", resourceId, resourceFileName, apName);
            Resource uploadedResource = resourceManager.saveResourceContent(resourceId, fis, resourceFileName, newResourceName, parameters.actorUser);
            uploadedResource.setOrigin(origin);
            uploadedResource.setOriginTimestamp(newOriginTimestamp);
            if (currentApId == null) {
                uploadedResource.addCustomField(MANUALLY_CREATED_AP_RESOURCE, true);
            }
            return resourceManager.saveResource(uploadedResource);
        } catch (IOException | InvalidResourceFormatException |
                 AutomationPackageUnsupportedResourceTypeException e) {
            throw new RuntimeException("Unable to create the resource for automation package", e);
        }
    }

    public static void checkResourceWriteAccess(String resourceFileName, Resource oldResource, WriteAccessValidator writeAccessValidator) {
        try {
            writeAccessValidator.validate(oldResource);
        } catch (ObjectAccessException e) {
            String errorMessage = "The existing resource " + oldResource.getId().toHexString() + " for file " + resourceFileName + " referenced by the provided package cannot be modified in the current context.";
            log.error(errorMessage);
            throw new AutomationPackageAccessException(errorMessage, e);
        }
    }

    protected void checkAccess(Resource resource, WriteAccessValidator writeAccessValidator) throws AutomationPackageAccessException {
        if (writeAccessValidator != null) {
            try {
                writeAccessValidator.validate(resource);
            } catch (ObjectAccessException e) {
                throw new AutomationPackageAccessException("You're not allowed to edit the linked automation package  " + getLogRepresentation(resource), e);
            }
        }
    }

    public RefreshResourceResult refreshResourceAndLinkedPackages(String resourceId,
                                                                  AutomationPackageUpdateParameter parameters,
                                                                  AutomationPackageManager apManager) {
        Resource resource = resourceManager.getResource(resourceId);
        if(resource == null){
            RefreshResourceResult result = new RefreshResourceResult();
            result.addError("Resource not found by id: " + resourceId);
            return result;
        }
        return refreshResourceAndLinkedPackages(resource, parameters.writeAccessValidator, (linkedAutomationPackages, refreshResourceResult) -> {
            List<AutomationPackage> reuploadedPackages = new ArrayList<>(linkedAutomationPackages);
            List<AutomationPackage> failedPackages = new ArrayList<>();
            String errorMessage = null;
            try {
                apManager.reloadRelatedAutomationPackages(
                        linkedAutomationPackages.stream().map(AbstractIdentifiableObject::getId).collect(Collectors.toSet()),
                        parameters
                );
            } catch (AutomationPackageRedeployException ex) {
                errorMessage = ex.getMessage();
                for (ObjectId failedId : ex.getFailedApsId()) {
                    AutomationPackage failedPackage = linkedAutomationPackages.stream().filter(ap -> ap.getId().equals(failedId)).findFirst().orElse(null);
                    if (failedPackage != null) {
                        reuploadedPackages.remove(failedPackage);
                        failedPackages.add(failedPackage);
                    }
                }
            }
            if (!reuploadedPackages.isEmpty()) {
                refreshResourceResult.addInfo("The following automation packages have been reuploaded: " + reuploadedPackages.stream().map(AutomationPackageManager::getLogRepresentation).collect(Collectors.toList()));
            }
            if (!failedPackages.isEmpty()) {
                refreshResourceResult.addError("Failed to reupload the following automation packages: " + failedPackages.stream().map(AutomationPackageManager::getLogRepresentation).collect(Collectors.toList())
                        + "; Reason: " + errorMessage);
            } else if (errorMessage != null) {
                refreshResourceResult.addError(errorMessage);
            }
        });
    }

    private RefreshResourceResult refreshResourceAndLinkedPackages(Resource resource,
                                                                  WriteAccessValidator writeAccessValidator,
                                                                  LinkedPackagesReuploader linkedPackagesReuploader) {
        RefreshResourceResult refreshResourceResult = new RefreshResourceResult();

        try {
            writeAccessValidator.validate(resource);
        } catch (ObjectAccessException e) {
            refreshResourceResult.addError("Access denied to resource " + resource.getId() + "; reason: " + e.getMessage());
            return refreshResourceResult;
        }

        if (!supportedResourceTypes.contains(resource.getResourceType())) {
            refreshResourceResult.addError("Unsupported resource type for refresh: " + resource.getResourceType() + ". Supported types: " + supportedResourceTypes);
        }
        if (!MavenArtifactIdentifier.isMvnIdentifierShortString(resource.getOrigin())) {
            refreshResourceResult.addError("Unsupported resource origin for refresh: " + resource.getOrigin() + ". Only maven artefact resources are supported");
        }

        //Here we already checked that we have write access to the refreshed resource, we allow updated all AP that are using it
        //So we get all APs using this resource and update them with no further access checks
        Set<AutomationPackage> linkedAutomationPackages
                = linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(resource.getId().toHexString(), new ArrayList<>())
                .stream()
                .map(automationPackageAccessor::get)
                .collect(Collectors.toSet());

        // DO NOTHING ON VALIDATION FAILURES
        if (refreshResourceResult.isFailed()) {
            return refreshResourceResult;
        }

        ResourceRevisionFileHandle fileHandle = resourceManager.getResourceFile(resource.getId().toString());
        boolean resourceFileExists = fileHandle != null && fileHandle.getResourceFile() != null && fileHandle.getResourceFile().exists();

        // REUPLOAD THE RESOURCE
        MavenArtifactIdentifier mavenArtifactIdentifier = MavenArtifactIdentifier.fromShortString(resource.getOrigin());
        try {
            if (!resourceFileExists) {
                // if file is missing in resource manager, we always download the actual content
                updateMavenFileContentInResourceManager(resource, mavenArtifactIdentifier, null);
                refreshResourceResult.setResultStatus(RefreshResourceResult.ResultStatus.REFRESHED);
            } else {
                // if file already exists, we don't need to download the actual content:
                // * for release artifacts (non-modifiable)
                // * for snapshots with the same remote metadata (not changed snapshots)
                if (mavenArtifactIdentifier.isModifiable()) {
                    SnapshotMetadata snapshotMetadata = mavenOperations.fetchSnapshotMetadata(mavenArtifactIdentifier, resource.getOriginTimestamp());
                    if (snapshotMetadata == null || snapshotMetadata.newSnapshotVersion) {
                        if (snapshotMetadata == null) {
                            log.warn("{} has no snapshot metadata and will be treated as if new content is available", mavenArtifactIdentifier.toStringRepresentation());
                        }
                        log.debug("New snapshot version found for {}, downloading it", mavenArtifactIdentifier.toStringRepresentation());
                        updateMavenFileContentInResourceManager(resource, mavenArtifactIdentifier, resource.getOriginTimestamp());
                        refreshResourceResult.setResultStatus(RefreshResourceResult.ResultStatus.REFRESHED);
                    } else {
                        // reuse resource
                        log.debug("Latest snapshot version already downloaded for {}, reusing it", mavenArtifactIdentifier.toStringRepresentation());
                        refreshResourceResult.addInfo("Refresh is not required for resource " + mavenArtifactIdentifier.toStringRepresentation() + ". The content of this resource is already up to date");
                        refreshResourceResult.setResultStatus(RefreshResourceResult.ResultStatus.NOT_REQUIRED);
                    }
                } else {
                    refreshResourceResult.addInfo("Refresh is not required for resource " + mavenArtifactIdentifier.toStringRepresentation() + ". The content of this resource is already up to date");
                    refreshResourceResult.setResultStatus(RefreshResourceResult.ResultStatus.NOT_REQUIRED);
                }
            }
        } catch (AutomationPackageReadingException e) {
            throw new AutomationPackageManagerException("Cannot restore the file from maven artifactory", e);
        }

        if (refreshResourceResult.getResultStatus() == RefreshResourceResult.ResultStatus.REFRESHED) {
            // REFRESH LINKED PACKAGES
            if (linkedPackagesReuploader != null) {
                linkedPackagesReuploader.reupload(linkedAutomationPackages, refreshResourceResult);
            }
        }

        return refreshResourceResult;
    }

    private void updateMavenFileContentInResourceManager(Resource resource, MavenArtifactIdentifier mavenArtifactIdentifier,
                                                         Long existingSnapshotTimestamp) {
        try {
            // restore the automation package file from maven
            ResolvedMavenArtifact resolvedMavenArtifact = mavenOperations.getFile(mavenArtifactIdentifier, existingSnapshotTimestamp);
            File file = resolvedMavenArtifact.artifactFile;
            try (FileInputStream fis = new FileInputStream(file)) {
                //The resource name doesn't change when saving new maven content for existing resource
                Resource updated = resourceManager.saveResourceContent(resource.getId().toHexString(), fis, file.getName(), resource.getAttribute(AbstractOrganizableObject.NAME), resource.getCreationUser());

                // update timestamp
                updated.setOriginTimestamp(Optional.ofNullable(resolvedMavenArtifact.snapshotMetadata).map(s -> s.timestamp).orElse(null));
                resourceManager.saveResource(updated);
            }
        } catch (InvalidResourceFormatException | IOException | AutomationPackageReadingException ex) {
            throw new AutomationPackageManagerException("Cannot restore the file from maven artifactory", ex);
        }
    }

    public void deleteResource(String resourceId, WriteAccessValidator writeAccessValidator) throws AutomationPackageAccessException, AutomationPackageUnsupportedResourceTypeException {
        Resource resource = resourceManager.getResource(resourceId);
        if (resource == null) {
            throw new AutomationPackageManagerException("Resource is not found by id: " + resourceId);
        }
        validateResourceType(resource);
        checkAccess(resource, writeAccessValidator);

        Set<ObjectId> linkedAps = linkedAutomationPackagesFinder.findAutomationPackagesIdsByResourceId(resourceId, List.of());

        if (!linkedAps.isEmpty()) {
            throw new AutomationPackageAccessException("Resource " + getLogRepresentation(resource) +
                    " cannot be deleted, because there are automation packages using this resource: " +
                    linkedAps.stream().map(p -> AutomationPackageManager.getLogRepresentation(automationPackageAccessor.get(p))).collect(Collectors.toList())
            );
        }

        resourceManager.deleteResource(resourceId);
    }

    private void validateResourceType(Resource resource) throws AutomationPackageUnsupportedResourceTypeException {
        if (!supportedResourceTypes.contains(resource.getResourceType())) {
            throw new AutomationPackageUnsupportedResourceTypeException(resource.getResourceType(), supportedResourceTypes);
        }
    }

    public List<AutomationPackage> findAutomationPackagesByResourceId(String resourceId, List<ObjectId> ignoredApIds) {
        return linkedAutomationPackagesFinder.findAutomationPackagesByResourceId(resourceId, ignoredApIds);
    }

    private static String getLogRepresentation(Resource r) {
        return "'" + r.getResourceName() + "'(" + r.getId() + ")";
    }

    /**
     * Search all revisions of the provided resource currently used by automation packages
     * Then call the resource manager to delete all other revisions except the current resource revisionId
     * @param resource the resource to clean-up
     */
    public void deleteUnusedResourceRevisions(Resource resource) {
        Set<String> usedRevision = new HashSet<>();
        String resourceId = resource.getId().toHexString();
        for (AutomationPackage automationPackage : findAutomationPackagesByResourceId(resourceId, List.of())) {
            if (Optional.ofNullable(automationPackage.getAutomationPackageResource()).filter(path -> path.contains(resourceId)).isPresent()) {
                usedRevision.add(FileResolver.resolveRevisionId(automationPackage.getAutomationPackageResourceRevision()));
            }
            if (Optional.ofNullable(automationPackage.getAutomationPackageLibraryResource()).filter(path -> path.contains(resourceId)).isPresent()) {
                usedRevision.add(FileResolver.resolveRevisionId(automationPackage.getAutomationPackageLibraryResourceRevision()));
            }
        }
        resourceManager.findAndCleanupUnusedRevision(resource, usedRevision);
    }

    public interface LinkedPackagesReuploader {
        void reupload(Set<AutomationPackage> linkedAutomationPackages,
                      RefreshResourceResult refreshResourceResult
        );
    }

    public interface MavenOperations {
        SnapshotMetadata fetchSnapshotMetadata(MavenArtifactIdentifier mavenArtifactIdentifier, Long existingSnapshotTimestamp) throws AutomationPackageReadingException;
        ResolvedMavenArtifact getFile(MavenArtifactIdentifier mavenArtifactIdentifier, Long existingSnapshotTimestamp) throws AutomationPackageReadingException;
    }

}
