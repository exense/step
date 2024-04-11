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
package step.core.controller.settings;

import org.bson.types.ObjectId;
import step.core.access.User;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.framework.server.Session;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static step.core.controller.settings.AbstractScopedObject.SCOPE_FIELD;

public class AbstractScopedObjectAccessor<T extends AbstractScopedObject> extends AbstractAccessor<T> {

    private final ObjectScopeRegistry objectScopeRegistry;

    public AbstractScopedObjectAccessor(Collection<T> collectionDriver, ObjectScopeRegistry objectScopeRegistry) {
        super(collectionDriver);
        this.objectScopeRegistry = objectScopeRegistry;
    }

    /**
     * Save object applying selected scope
     * @param baseScope the map of base scope info to be saved
     * @param scopedObject the scoped object to be saved
     * @param scopes the scope to be applied from the session context
     * @param session the related session (used to retrieve scope values)
     */
    public T saveScopedObject(Map<String, String> baseScope, T scopedObject, List<String> scopes, Session<User> session) {
        //Override previous scope and id in case of update, i.e. if scope is changed we create a new entry
        AbstractIdentifiableObject previousScopedObject = get(scopedObject.getId());
        if (previousScopedObject != null) {
            scopedObject.setId(new ObjectId());
        }
        scopedObject.setScope(Objects.requireNonNullElse(new HashMap<>(baseScope), new HashMap<>()));
        //Enrich with requested scope based on session context
        objectScopeRegistry.addScopesToObject(scopedObject, scopes, session);
        // Cleaning up any existing objects with narrower scope. Example: an admin change the table settings for the
        // whole system, this will delete the settings saved with more specific  scope (user and/or project)
        cleanupObjectsWithNarrowerScopes(scopedObject, session);
        //Check if an entry exists for this exact combination of scope, and set for update in this case
        Optional<T> byScope = (Optional<T>) getByScope(scopedObject.getScope());
        byScope.ifPresent(s -> scopedObject.setId(s.getId()));
        //Finally save or update
        return save(scopedObject);
    }

    private void cleanupObjectsWithNarrowerScopes(T scopedObject, Session<User> session) {
        //any entry saved with the same scope info + additional scope info are cleaned up
        List<T> baseScopedObjects = this.collectionDriver.find(getBaseFilters(scopedObject.getScope()), null, null, null, 0).collect(Collectors.toList());
        baseScopedObjects.stream().filter(o -> o.getScope().size() > scopedObject.getScope().size()).forEach(o->this.remove(o.getId()));
    }

    /**
     * Retrieve the object having the requested base scope and with the highest priority given the current session context.
     * Example: one object with base scope settingId=A is stored for scope project=projectA and user=userA and another one only with scope user=userA.
     * If the session is for userA in project A, the first setting will be returned, if the session is for userA in projectB,
     * the 2nd setting will be returned, otherwise no setting will be returned
     * @param baseScope the map of static scope information (i.e. defined not the caller and not based on session context
     * @param session the session used to match stored objects by scope
     * @return optional scoped object
     */
    public Optional<T> getScopedObject(Map<String, String> baseScope, Session<User> session) {
        Optional<T> first = Optional.empty();
        Filter baseFilters = getBaseFilters(baseScope);
        List<T> scopedObjects = this.collectionDriver.find(baseFilters, null, null, null, 0).collect(Collectors.toList());
        List<List<Predicate<AbstractScopedObject>>> predicateListsInPriorityOrder = objectScopeRegistry.getPredicateListsInPriorityOrder(session);
        for (List<Predicate<AbstractScopedObject>> predicateList : predicateListsInPriorityOrder) {
            first = scopedObjects.stream().filter(predicateList.stream().reduce(Predicate::and).orElse(x -> false)).findFirst();
            if (first.isPresent()) {
                break;
            }
        }
        return first;
    }

    private Filter getBaseFilters(Map<String, String> baseScope) {
        return Filters.and(baseScope.entrySet().stream().map(e -> Filters.equals(SCOPE_FIELD + "." + e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    private Optional<T> getByScope(Map<String, String> scopeMap) {
        //Return element which has the exact same scope, all element of the map matches and not other scope is defined
        Filter baseFilters = getBaseFilters(scopeMap);
        return this.collectionDriver.find(baseFilters, null, null, null, 0).filter(o -> o.getScope().size() == scopeMap.size()).findFirst();
    }
}
