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
import step.attachments.FileResolver;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.library.AutomationPackageLibraryProvider;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.WriteAccessValidator;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;

import java.util.*;
import java.util.stream.Collectors;

public class LinkedAutomationPackagesFinder {

    private final ResourceManager resourceManager;
    private final AutomationPackageAccessor automationPackageAccessor;

    public LinkedAutomationPackagesFinder(ResourceManager resourceManager, AutomationPackageAccessor automationPackageAccessor) {
        this.resourceManager = resourceManager;
        this.automationPackageAccessor = automationPackageAccessor;
    }

    public ConflictingAutomationPackages findConflictingPackages(AutomationPackageArchiveProvider automationPackageProvider, ObjectPredicate objectPredicate,
                                                                 AutomationPackageLibraryProvider apLibraryProvider, boolean checkForSameOrigin, AutomationPackage oldPackage) {
        ConflictingAutomationPackages conflictingAutomationPackages;
        if (checkForSameOrigin) {
            conflictingAutomationPackages = findConflictingAutomationPackages(apLibraryProvider, automationPackageProvider, oldPackage, objectPredicate);
        } else {
            conflictingAutomationPackages = new ConflictingAutomationPackages();
        }
        return conflictingAutomationPackages;
    }

    private ConflictingAutomationPackages findConflictingAutomationPackages(AutomationPackageLibraryProvider apLibProvider,
                                                                            AutomationPackageArchiveProvider automationPackageProvider,
                                                                            AutomationPackage oldPackage,
                                                                            ObjectPredicate objectPredicate) {
        ConflictingAutomationPackages conflictingAutomationPackages = new ConflictingAutomationPackages();

        // when we search the conflicting automation packages by origin (by AP file or by used keyword library), we only need to take into account
        // the libraries with identifiable (to search the resource by origin) and modifiable (i.e. SNAPSHOT) origins,
        // because for non-identifiable origins we will always upload the new resource and for unmodifiable origins - reuse the exiting resource
        // (both these cases are not conflicting and don't require the additional confirmation from user)
        Set<ObjectId> automationPackagesWithSameOrigin = new HashSet<>();
        ResourceOrigin apOrigin = automationPackageProvider.getOrigin();
        if (apOrigin != null && automationPackageProvider.canLookupResources() && automationPackageProvider.isModifiableResource() && automationPackageProvider.hasNewContent()) {
            Resource resourcesByOrigin = resourceManager.getResourceByNameAndType(apOrigin.toStringRepresentation(), ResourceManager.RESOURCE_TYPE_AP, objectPredicate);
            if (resourcesByOrigin != null) {
              automationPackagesWithSameOrigin.addAll(findAutomationPackagesIdsByResourceId(resourcesByOrigin.getId().toHexString(), oldPackage == null ? List.of() : List.of(oldPackage.getId())));
            }
            conflictingAutomationPackages.setApWithSameOrigin(automationPackagesWithSameOrigin);
        }

        Set<ObjectId> apWithSameLibrary = new HashSet<>();
        ResourceOrigin apLibOrigin = apLibProvider == null ? null : apLibProvider.getOrigin();
        if (apLibOrigin != null && apLibProvider.canLookupResources() && apLibProvider.isModifiableResource() && apLibProvider.hasNewContent()) {
            Resource resourcesByOrigin = resourceManager.getResourceByNameAndType(apLibOrigin.toStringRepresentation(), ResourceManager.RESOURCE_TYPE_AP_LIBRARY, objectPredicate);
            if (resourcesByOrigin != null) {
                apWithSameLibrary.addAll(findAutomationPackagesIdsByResourceId(resourcesByOrigin.getId().toHexString(), oldPackage == null ? List.of() : List.of(oldPackage.getId())));
            }
            conflictingAutomationPackages.setApWithSameLibrary(new HashSet<>(apWithSameLibrary));
        }
        return conflictingAutomationPackages;
    }

    public Set<ObjectId> findAutomationPackagesIdsByResourceId(String resourceId, List<ObjectId> ignoredApIds) {
        Set<ObjectId> result = new HashSet<>();
        result.addAll(automationPackageAccessor.findByAutomationPackageResource(FileResolver.RESOURCE_PREFIX + resourceId).stream()
                .map(AbstractIdentifiableObject::getId)
                .filter(ap -> !ignoredApIds.contains(ap))
                .collect(Collectors.toList()));
        result.addAll(automationPackageAccessor.findByLibraryResource(FileResolver.RESOURCE_PREFIX + resourceId)
                .stream().map(AbstractIdentifiableObject::getId)
                .filter(ap -> !ignoredApIds.contains(ap))
                .collect(Collectors.toList()));
        return result;
    }

    public List<AutomationPackage> findAutomationPackagesByResourceId(String resourceId, List<ObjectId> ignoredApIds) {
        return findAutomationPackagesIdsByResourceId(resourceId, ignoredApIds)
                .stream()
                .map(automationPackageAccessor::get)
                .sorted(Comparator.comparing(o -> o.getAttribute(AbstractOrganizableObject.NAME)))
                .collect(Collectors.toList());
    }
}
