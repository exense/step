package step.automation.packages.library;

import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.ManagedLibraryMissingException;
import step.core.objectenricher.ObjectPredicate;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class ManagedLibraryProvider implements AutomationPackageLibraryProvider {

    /**
     * For managed library when creating or updating a resource, a source provider is set and used to retrieve the content
     * Otherwise when deploying or executing an AP using a managed library we fall back to the default resource id provider
     */
    private final AutomationPackageLibraryProvider sourceContentProvider;
    private final Resource existingManagedLibrary;
    private final boolean managingLibrary;
    private final String managedLibraryName;

    public ManagedLibraryProvider(ResourceManager resourceManager, String managedLibraryName, ObjectPredicate objectPredicate) throws ManagedLibraryMissingException {
        this.existingManagedLibrary = getManagedLibraryResource(resourceManager, managedLibraryName, objectPredicate);
        this.sourceContentProvider = new AutomationPackageLibraryFromResourceIdProvider(resourceManager, existingManagedLibrary.getId().toHexString(), objectPredicate);
        this.managingLibrary = false;
        this.managedLibraryName = managedLibraryName;
    }

    public ManagedLibraryProvider(AutomationPackageLibraryProvider sourceContentProvider, Resource resource, String managedLibraryName) throws ManagedLibraryMissingException {
        this.sourceContentProvider = Objects.requireNonNull(sourceContentProvider);
        this.existingManagedLibrary = resource;
        this.managingLibrary = true;
        this.managedLibraryName = managedLibraryName;
    }

    public static Resource getManagedLibraryResource(ResourceManager resourceManager, String managedLibraryName, ObjectPredicate objectPredicate) throws ManagedLibraryMissingException {
        Resource resource = resourceManager.getResourceByNameAndType(managedLibraryName, ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY, objectPredicate);
        if (resource == null) {
            throw new ManagedLibraryMissingException(managedLibraryName);
        }
        return resource;
    }

    @Override
    public String getResourceType() {
        return ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY;
    }

    @Override
    public boolean isModifiableResource() {
        //In case of direct update of the library, we allow modification of managed library content
        return managingLibrary || sourceContentProvider.isModifiableResource();
    }

    @Override
    public boolean canLookupResources() {
        //In case of direct update of the library, we allow modification of managed library content
        return managingLibrary || sourceContentProvider.canLookupResources();
    }

    @Override
    public File getAutomationPackageLibrary() throws AutomationPackageReadingException {
        return sourceContentProvider.getAutomationPackageLibrary();
    }

    @Override
    public ResourceOrigin getOrigin() {
        return sourceContentProvider.getOrigin();
    }

    @Override
    public String getResourceName() {
        return managedLibraryName;
    }

    @Override
    public Optional<Resource> lookupExistingResource(ResourceManager resourceManager, ObjectPredicate objectPredicate) {
        //In case of direct update of the library, we allow modification of managed library content
        if (managingLibrary) {
            return Optional.ofNullable(existingManagedLibrary);
        } else {
            return sourceContentProvider.lookupExistingResource(resourceManager, objectPredicate);
        }
    }

    @Override
    public Long getSnapshotTimestamp() {
        return sourceContentProvider.getSnapshotTimestamp();
    }

    @Override
    public boolean hasNewContent() {
        //In case of direct creation/update of the managed library there is always new content otherwise delegate to the source content provider
        if (managingLibrary) {
            return true;
        } else {
            return sourceContentProvider.hasNewContent();
        }
    }

    @Override
    public void close() throws IOException {
        sourceContentProvider.close();
    }
}
