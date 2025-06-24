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
package step.unique;

import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.objectenricher.EnricheableObject;

import java.util.*;

public class EntityWithUniqueAttributesAccessor<T extends AbstractIdentifiableObject> extends AbstractAccessor<T> {

    public EntityWithUniqueAttributesAccessor(Collection<? extends EnricheableObject> collectionDriver) {
        super((Collection<T>) collectionDriver);
    }

    public Optional<T> findDuplicate(EntityWithUniqueAttributes e, Set<String> ignoredAttributes){
        EnricheableObject enricheableObject = (EnricheableObject) e;

        ArrayList<Filter> attrFilter = new ArrayList<>();
        if (enricheableObject.getAttributes() != null && !enricheableObject.getAttributes().isEmpty()) {
            for (Map.Entry<String, String> entry : enricheableObject.getAttributes().entrySet()) {
                if(ignoredAttributes == null || !ignoredAttributes.contains(entry.getKey())) {
                    attrFilter.add(Filters.equals("attributes." + entry.getKey(), entry.getValue()));
                }
            }
        }

        Filter filterByAttribute = Filters.and(attrFilter);
        if(e.getKeyFieldName() != null){
            attrFilter.add(Filters.equals(e.getKeyFieldName(), e.getKey()));
        }

        // filter out the entity with the same ID and apply the filter by key again (if getKeyFieldName returns null and the key is resolved dynamically)
        return getCollectionDriver().find(filterByAttribute, null, null, null, 0)
                .filter(tmp -> !Objects.equals(((AbstractIdentifiableObject) e).getId(), tmp.getId()) && Objects.equals(e.getKey(), ((EntityWithUniqueAttributes) tmp).getKey()))
                .findFirst();
    }


}
