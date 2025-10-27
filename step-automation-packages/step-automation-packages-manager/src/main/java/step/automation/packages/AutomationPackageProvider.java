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

import step.core.objectenricher.ObjectPredicate;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

public interface AutomationPackageProvider extends Closeable {

    ResourceOrigin getOrigin();

    default boolean isModifiableResource() {
        return getOrigin() != null && getOrigin().isModifiable();
    }

    default boolean canLookupResources() {
        return getOrigin() != null && getOrigin().isIdentifiable();
    }

    default Optional<Resource> lookupExistingResource(ResourceManager resourceManager, ObjectPredicate objectPredicate) {
        // by default, we look up resources by resource origin
        if (canLookupResources() && getOrigin() != null) {
            //Since the introduction of managed library, the resource name and origin are aligned for non managed library. It is allowed to have 2 library with the same origin if one is managed and antoher one is unmanaged
            Resource resourcesByOrigin = resourceManager.getResourceByNameAndType(getOrigin().toStringRepresentation(), getResourceType(), objectPredicate);
            // Resource by origin are unique and only so at max one resource should be found. It is however not the role of this method to validate it, so no check is done here
            return (resourcesByOrigin == null) ?  Optional.empty() : Optional.of(resourcesByOrigin);
        } else {
            throw new UnsupportedOperationException("Resources cannot be looked up for provider " + this.getClass().getSimpleName());
        }
    }

    default Long getSnapshotTimestamp() {
        return null;
    }

    default boolean hasNewContent() {
        return false;
    }

    default String getResourceType() {
        return ResourceManager.RESOURCE_TYPE_AP;
    }
}
