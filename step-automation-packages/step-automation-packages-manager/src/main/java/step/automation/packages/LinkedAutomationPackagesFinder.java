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
import step.automation.packages.kwlibrary.AutomationPackageLibraryProvider;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.objectenricher.ObjectPredicate;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LinkedAutomationPackagesFinder {

    private final ResourceManager resourceManager;
    private final AutomationPackageAccessor automationPackageAccessor;

    public LinkedAutomationPackagesFinder(ResourceManager resourceManager, AutomationPackageAccessor automationPackageAccessor) {
        this.resourceManager = resourceManager;
        this.automationPackageAccessor = automationPackageAccessor;
    }

    public ConflictingAutomationPackages findConflictingPackagesAndCheckAccess(AutomationPackageArchiveProvider automationPackageProvider, ObjectPredicate objectPredicate,
                                                                               ObjectPredicate writeAccessPredicated, AutomationPackageLibraryProvider keywordLibraryProvider, boolean allowUpdateOfOtherPackages, boolean checkForSameOrigin, AutomationPackage oldPackage, AutomationPackageManager automationPackageManager) {
        ConflictingAutomationPackages conflictingAutomationPackages;
        if (checkForSameOrigin) {
            conflictingAutomationPackages = findConflictingAutomationPackages(keywordLibraryProvider, automationPackageProvider, oldPackage, objectPredicate);
        } else {
            conflictingAutomationPackages = new ConflictingAutomationPackages();
        }

        List<ObjectId> apsForReupload = conflictingAutomationPackages.getApWithSameOrigin();
        if (!allowUpdateOfOtherPackages) {
            if (conflictingAutomationPackages.apWithSameOriginExists() || conflictingAutomationPackages.apWithSameKeywordLibExists()) {
                throw new AutomationPackageCollisionException(apsForReupload, conflictingAutomationPackages.getApWithSameKeywordLib());
            }
        } else {
            // even if allowUpdateOfOtherPackages flag is set we have to check if current user has enough permissions to modify these automation packages
            if (apsForReupload != null) {
                for (ObjectId apId : apsForReupload) {
                    AutomationPackage apForReupload = automationPackageManager.automationPackageAccessor.get(apId);
                    automationPackageManager.checkAccess(apForReupload, writeAccessPredicated);
                }
            }
        }
        return conflictingAutomationPackages;
    }

    private ConflictingAutomationPackages findConflictingAutomationPackages(AutomationPackageLibraryProvider kwLibProvider,
                                                                            AutomationPackageArchiveProvider automationPackageProvider,
                                                                            AutomationPackage oldPackage,
                                                                            ObjectPredicate objectPredicate) {
        ConflictingAutomationPackages conflictingAutomationPackages = new ConflictingAutomationPackages();

        // when we search the conflicting automation packages by origin (by AP file or by used keyword library), we only need to take into account
        // the libraries with identifiable (to search the resource by origin) and modifiable (i.e. SNAPSHOT) origins,
        // because for non-identifiable origins we will always upload the new resource and for unmodifiable origins - reuse the exiting resource
        // (both these cases are not conflicting and don't require the additional confirmation from user)
        List<ObjectId> automationPackagesWithSameOrigin = new ArrayList<>();
        ResourceOrigin apOrigin = automationPackageProvider.getOrigin();
        if (apOrigin != null && automationPackageProvider.canLookupResources() && automationPackageProvider.isModifiableResource()) {
            List<Resource> resourcesByOrigin = resourceManager.getResourcesByOrigin(apOrigin.toStringRepresentation(), objectPredicate);
            for (Resource resource : resourcesByOrigin) {
              automationPackagesWithSameOrigin.addAll(findAutomationPackagesByResourceId(resource.getId().toHexString(), oldPackage == null ? List.of() : List.of(oldPackage.getId())));
            }
            conflictingAutomationPackages.setApWithSameOrigin(automationPackagesWithSameOrigin);
        }

        Set<ObjectId> apWithSameKeywordLib = new HashSet<>();
        ResourceOrigin keywordLibOrigin = kwLibProvider == null ? null : kwLibProvider.getOrigin();
        if (keywordLibOrigin != null && kwLibProvider.canLookupResources() && kwLibProvider.isModifiableResource()) {
            List<Resource> resourcesByOrigin = resourceManager.getResourcesByOrigin(keywordLibOrigin.toStringRepresentation(), objectPredicate);
            for (Resource resource : resourcesByOrigin) {
                apWithSameKeywordLib.addAll(findAutomationPackagesByResourceId(resource.getId().toHexString(), oldPackage == null ? List.of() : List.of(oldPackage.getId())));
            }
            conflictingAutomationPackages.setApWithSameKeywordLib(new ArrayList<>(apWithSameKeywordLib));
        }
        return conflictingAutomationPackages;
    }

    public Set<ObjectId> findAutomationPackagesByResourceId(String resourceId, List<ObjectId> ignoredApIds){
        Set<ObjectId> result = new HashSet<>();
        result.addAll(automationPackageAccessor.findByAutomationPackageResource(FileResolver.RESOURCE_PREFIX + resourceId).stream()
                .map(AbstractIdentifiableObject::getId)
                .filter(ap -> !ignoredApIds.contains(ap))
                .collect(Collectors.toList()));
        result.addAll(automationPackageAccessor.findByKeywordLibResource(FileResolver.RESOURCE_PREFIX + resourceId)
                .stream().map(AbstractIdentifiableObject::getId)
                .filter(ap -> !ignoredApIds.contains(ap))
                .collect(Collectors.toList()));
        return result;
    }
}
