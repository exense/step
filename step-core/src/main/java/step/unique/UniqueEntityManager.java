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
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectValidator;

import java.util.*;

public class UniqueEntityManager {

    public ObjectValidator createObjectValidator(CollectionFactory collectionFactory) {
        return enricheableObject -> {
            if (enricheableObject instanceof EntityWithUniqueAttributes) {
                Collection<? extends EnricheableObject> collection = collectionFactory.getCollection(((EntityWithUniqueAttributes) enricheableObject).getEntityName(), enricheableObject.getClass());
                EntityWithUniqueAttributesAccessor<? extends AbstractIdentifiableObject> accessor = new EntityWithUniqueAttributesAccessor<>(collection);
                Optional<? extends EntityWithUniqueAttributes> duplicate = (Optional<? extends EntityWithUniqueAttributes>) accessor.findDuplicate((EntityWithUniqueAttributes) enricheableObject);
                if (duplicate.isPresent()) {
                    throw new RuntimeException(String.format("%s (%s) cannot be saved. Another entity (%s) with the same attributes has been detected",
                            ((EntityWithUniqueAttributes) enricheableObject).getEntityName(), ((AbstractIdentifiableObject) enricheableObject).getId(), ((AbstractIdentifiableObject) duplicate.get()).getId()
                    ));
                }

            }
        };
    }
}
