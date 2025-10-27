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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AutomationPackageResourceManager {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageResourceManager.class);
    public static final String MANUALLY_CREATED_AP_RESOURCE = "MANUALLY_CREATED_AP_RESOURCE";
    private final ResourceManager resourceManager;
    private final AutomationPackageOperationMode operationMode;
    private final AutomationPackageAccessor automationPackageAccessor;
    private final LinkedAutomationPackagesFinder linkedAutomationPackagesFinder;
    private final AutomationPackageMavenConfig mavenConfig;
    private final Set<String> supportedResourceTypes = Set.of(
            ResourceManager.RESOURCE_TYPE_AP,
            ResourceManager.RESOURCE_TYPE_AP_LIBRARY,
            ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY
    );

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
                            throw new AutomationPackageManagerException("Old resource " + oldResource.getResourceName() + " ( " + oldResource.getId() + " ) has been detected and cannot be reused");
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
                                                          AutomationPackageUpdateParameter parameters, boolean allowToReuseOldResource, String optionalResourceName, boolean allowUpdateContent) {
        String apName = automationPackageToBeLinkedWithLib == null ? "" : automationPackageToBeLinkedWithLib.getAttribute(AbstractOrganizableObject.NAME);
        String apLibraryResourceString = null;
        Resource uploadedResource = null;
        try {
            File apLibrary = apLibProvider.getAutomationPackageLibrary();
            if (apLibrary != null) {
                // for isolated execution we always use the isolatedAp resource type to support auto cleanup after execution
                String resourceType = this.operationMode == AutomationPackageOperationMode.ISOLATED ? ResourceManager.RESOURCE_TYPE_ISOLATED_AP_LIB : apLibProvider.getResourceType();

                // we can reuse the existing old resource in case it is identifiable (can be found by origin) and unmodifiable
                Optional<Resource> oldResources = Optional.empty();
                if (apLibProvider.canLookupResources()) {
                    oldResources = apLibProvider.lookupExistingResource(resourceManager, parameters.objectPredicate);
                }

                if (oldResources.isPresent()) {
                    Resource oldResource = oldResources.get();

                    if(!allowToReuseOldResource){
                        throw new AutomationPackageManagerException("Old resource " + oldResource.getResourceName() + " ( " + oldResource.getId() + " ) has been detected and cannot be reused");
                    }

                    if (!apLibProvider.isModifiableResource()) {
                        // for unmodifiable origins we just reused the previously uploaded resource
                        log.info("Existing automation package library {} with resource id {} has been detected and will be reused in AP {}", apLibrary.getName(), oldResource.getId().toHexString(), apName);
                        uploadedResource = oldResource;
                    } else if (apLibProvider.hasNewContent() && allowUpdateContent) {
                        // for modifiable resources (i.e. SNAPSHOTS) we can reuse the old resource id and metadata, but we need to update the content if a new version was downloaded
                        try (FileInputStream fis = new FileInputStream(apLibrary)) {
                            uploadedResource = updateExistingResourceContentAndPropagate(apLibrary.getName(),
                                    apName,
                                    automationPackageToBeLinkedWithLib == null ? null : automationPackageToBeLinkedWithLib.getId(),
                                    fis, apLibProvider.getSnapshotTimestamp(), oldResource, optionalResourceName,
                                    parameters.actorUser, parameters.writeAccessValidator
                            );
                        }
                    } else {
                        uploadedResource = oldResource;
                    }
                } else {
                    ResourceOrigin origin = apLibProvider.getOrigin();

                    // old resource is not found - we create a new one
                    try (FileInputStream fis = new FileInputStream(apLibrary)) {
                        uploadedResource = resourceManager.createTrackedResource(
                                resourceType, false, fis, apLibrary.getName(), optionalResourceName, parameters.enricher, null,
                                parameters.actorUser, origin == null ? null : origin.toStringRepresentation(),
                                apLibProvider.getSnapshotTimestamp()
                        );
                        log.info("The new automation package library ({}) has been uploaded as ({})", apLibProvider, uploadedResource.getId().toHexString());
                    }
                }
                if(automationPackageToBeLinkedWithLib != null) {
                    apLibraryResourceString = FileResolver.RESOURCE_PREFIX + uploadedResource.getId().toString();
                    automationPackageToBeLinkedWithLib.setAutomationPackageLibraryResource(apLibraryResourceString);
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
                                            AutomationPackageUpdateParameter parameters,
                                            boolean allowToReuseOldResource, boolean allowUpdateContent) {
        ResourceOrigin apOrigin = apProvider.getOrigin();
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
                throw new AutomationPackageManagerException("Old resource " + resource.getResourceName() + " ( " + resource.getId() + " ) has been detected and cannot be reused");
            }

            // we just reuse the existing resource of unmodifiable origin (i.e non-SNAPSHOT)
            // and for SNAPSHOT we keep the same resource id, but update the content if a new version was found
            if (apProvider.isModifiableResource() && apProvider.hasNewContent() && allowUpdateContent) {
                try (FileInputStream is = new FileInputStream(originalFile)) {
                    resource = updateExistingResourceContentAndPropagate(
                            originalFile.getName(),
                            apName, automationPackageToBeLinkedWithResource == null ? null : automationPackageToBeLinkedWithResource.getId(),
                            is, apProvider.getSnapshotTimestamp(), resource, null,
                            parameters.actorUser, parameters.writeAccessValidator
                    );
                } catch (IOException | InvalidResourceFormatException |
                         AutomationPackageUnsupportedResourceTypeException e) {
                    throw new RuntimeException("Unable to create the resource for automation package", e);
                }
            }
        }

        // create the new resource if the old one cannot be reused
        if (resource == null) {
            try (InputStream is = new FileInputStream(originalFile)) {
                resource = resourceManager.createTrackedResource(
                        ResourceManager.RESOURCE_TYPE_AP, false, is, originalFile.getName(), parameters.enricher, null, parameters.actorUser,
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
     * @param writeAccessValidator validator to check if this resource can be updated in this context
     * @return the updated resource
     * @throws InvalidResourceFormatException in case the new resource content is invalid
     * @throws AutomationPackageAccessException in case the resource of linked AP cannot be updated in the current context
     */
    private Resource updateExistingResourceContentAndPropagate(String resourceFileName,
                                                               String apName, ObjectId currentApId,
                                                               FileInputStream fis, Long newOriginTimestamp,
                                                               Resource oldResource, String newResourceName, String actorUser,
                                                               WriteAccessValidator writeAccessValidator) throws IOException, InvalidResourceFormatException, AutomationPackageAccessException, AutomationPackageUnsupportedResourceTypeException {
        String resourceId = oldResource.getId().toHexString();
        //Check write access to the resource itself
        checkResourceWriteAccess(resourceFileName, oldResource, writeAccessValidator);
        validateResourceType(oldResource);

        log.info("Existing resource {} for file {} will be actualized and reused in AP {}", resourceId, resourceFileName, apName);
        Resource uploadedResource = resourceManager.saveResourceContent(resourceId, fis, resourceFileName, newResourceName, actorUser);
        uploadedResource.setOriginTimestamp(newOriginTimestamp);
        if (currentApId == null) {
            uploadedResource.addCustomField(MANUALLY_CREATED_AP_RESOURCE, true);
        }
        resourceManager.saveResource(uploadedResource);
        return uploadedResource;
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
                Long newSnapshotTimestamp = null;
                if (mavenArtifactIdentifier.isModifiable()) {
                    SnapshotMetadata snapshotMetadata = MavenArtifactDownloader.fetchSnapshotMetadata(mavenConfig, mavenArtifactIdentifier, resource.getOriginTimestamp());
                    if(snapshotMetadata != null){
                        newSnapshotTimestamp = snapshotMetadata.timestamp;
                    }
                }
                saveMavenFileContentInResourceManager(resource, mavenArtifactIdentifier, null);
                refreshResourceResult.setResultStatus(RefreshResourceResult.ResultStatus.REFRESHED);
            } else {
                // if file already exists, we don't need to download the actual content:
                // * for release artifacts (non-modifiable)
                // * for snapshots with the same remote metadata (not changed snapshots)
                if (mavenArtifactIdentifier.isModifiable()) {
                    SnapshotMetadata snapshotMetadata = MavenArtifactDownloader.fetchSnapshotMetadata(mavenConfig, mavenArtifactIdentifier, resource.getOriginTimestamp());
                    if (snapshotMetadata.newSnapshotVersion) {
                        log.debug("New snapshot version found for {}, downloading it", mavenArtifactIdentifier.toStringRepresentation());
                        saveMavenFileContentInResourceManager(resource, mavenArtifactIdentifier, resource.getOriginTimestamp());
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

    private void saveMavenFileContentInResourceManager(Resource resource, MavenArtifactIdentifier mavenArtifactIdentifier,
                                                       Long existingSnapshotTimestamp) {
        try {
            // restore the automation package file from maven
            ResolvedMavenArtifact resolvedMavenArtifact = MavenArtifactDownloader.getFile(mavenConfig, mavenArtifactIdentifier, existingSnapshotTimestamp);
            File file = resolvedMavenArtifact.artifactFile;
            try (FileInputStream fis = new FileInputStream(file)) {
                Resource updated = resourceManager.saveResourceContent(resource.getId().toHexString(), fis, file.getName(), null, resource.getCreationUser());

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
        validateResourceType(resource);
        if (resource == null) {
            throw new AutomationPackageManagerException("Resource is not found by id: " + resourceId);
        }
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

    public interface LinkedPackagesReuploader {
        void reupload(Set<AutomationPackage> linkedAutomationPackages,
                      RefreshResourceResult refreshResourceResult
        );
    }

}
