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

import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.objectenricher.ObjectValidator;

import java.util.*;

public class UniqueEntityManager {

    public ObjectValidator createObjectValidator(CollectionFactory collectionFactory) {
        return enricheableObject -> {
            if (enricheableObject instanceof EntityWithUniqueAttributes) {
                EntityWithUniqueAttributes e = (EntityWithUniqueAttributes) enricheableObject;
                Collection<? extends EntityWithUniqueAttributes> collection = collectionFactory.getCollection(e.getEntityName(), e.getClass());

                if (!enricheableObject.getAttributes().isEmpty()) {
                    ArrayList<Filter> attrFilter = new ArrayList<>();
                    for (Map.Entry<String, String> entry : enricheableObject.getAttributes().entrySet()) {
                        attrFilter.add(Filters.equals("attributes." + entry.getKey(), entry.getValue()));
                    }
                    Filter filterByAttribute = Filters.and(attrFilter);
                    Optional<? extends EntityWithUniqueAttributes> collision = collection.find(filterByAttribute, null, null, null, 0)
                            .filter(tmp -> !Objects.equals(((AbstractIdentifiableObject) e).getId(), ((AbstractIdentifiableObject) tmp).getId()) && Objects.equals(e.getKey(), tmp.getKey()))
                            .findFirst();
                    if (collision.isPresent()) {
                        throw new RuntimeException(String.format("%s (%s) cannot be saved. Another entity (%s) with the same attributes has been detected",
                                e.getEntityName(), ((AbstractIdentifiableObject) e).getId(), ((AbstractIdentifiableObject) collision.get()).getId()
                        ));
                    }
                }
            }
        };
    }
}
