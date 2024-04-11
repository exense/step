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

import step.core.access.User;
import step.framework.server.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ObjectScopeRegistry {

    private final Map<String, ObjectScopeHandler> objectScopeHandlers = new ConcurrentHashMap<>();

    public void register(String scope, ObjectScopeHandler objectScopeHandler) {
        objectScopeHandlers.put(scope, objectScopeHandler);
    }

    public void addScopesToObject(AbstractScopedObject abstractScopeObject, List<String> scopes, Session<User> session) {
        Objects.requireNonNull(scopes, "Scope list cannot be null, use an empty list instead");
        scopes.forEach(s -> {
            ObjectScopeHandler objectScopeHandler = objectScopeHandlers.get(s);
            if (objectScopeHandler != null) {
                objectScopeHandler.saveScopeDetails(abstractScopeObject, session);
            } else {
                throw new UnsupportedOperationException("The scope '" + s + "' is not registered");
            }
        });
    }

    /**
     * Build filters for requested scope and given session context, unrequested scope must not exist in the records
     * @param scopes requested scope
     * @param session session context from which scope values are retrieved
     * @return the list of filters
     */
    public List<Predicate<AbstractScopedObject>> getPredicates(List<ObjectScopeHandler> scopes, Session session) {
        Objects.requireNonNull(scopes, "Scope list cannot be null, use an empty list instead");
        return  objectScopeHandlers.values().stream()
                .map(h -> (scopes.contains(h)) ? h.getObjectPredicate(session) : h.getObjectPredicateIsNotSet())
                .collect(Collectors.toList());
    }


    /**
     * This method return the list of filters to be used to retrieve objects by scope
     * The list is in priority order, the last filter search for the entry with no scope information (application scope)
     * @param session the Step HTTP session from which the scope is extracted
     * @return the list of ordered filters to retrieve the objects
     */
    protected List<List<Predicate<AbstractScopedObject>>> getPredicateListsInPriorityOrder(Session<User> session) {
        //Get available scope information in Session and retrieve applicable handlers
        List<List<Predicate<AbstractScopedObject>>> listOfScopedObjectPredicates = new ArrayList<>();
        List<ObjectScopeHandler> scopeHandlers = objectScopeHandlers.values().stream()
                .filter(objectScopeHandler -> objectScopeHandler.scopeIsApplicable(session)).collect(Collectors.toList());

        //List all scope combinations, sort them from highest to lower priorities and return corresponding filters
        listOfScopedObjectPredicates.addAll(getAllCombination(scopeHandlers).stream().sorted((o1, o2) -> {
            Integer o1AggPriorities = o1.stream().map(ObjectScopeHandler::getPriority).reduce(0, Integer::sum);
            Integer o2AggPriorities = o2.stream().map(ObjectScopeHandler::getPriority).reduce(0, Integer::sum);
            return o2AggPriorities - o1AggPriorities;
        }).map(l -> getPredicates(l, session)).collect(Collectors.toList()));

        //Final filter is the empty filter to search for application scope
        listOfScopedObjectPredicates.add(getPredicates(List.of(), session));
        return listOfScopedObjectPredicates;
    }

    private List<List<ObjectScopeHandler>> getAllCombination(List<ObjectScopeHandler> scopeHandlers) {
        int size = scopeHandlers.size();
        List<List<ObjectScopeHandler>> results = new ArrayList<>();

        for (int i = size; i > 0; i--) {
            ObjectScopeHandler[] nResults = new ObjectScopeHandler[i];
            getAllNCombination(scopeHandlers, results, nResults, 0, 0, i);
        }
        return results;
    }

    private void getAllNCombination(List<ObjectScopeHandler> scopeHandlers, List<List<ObjectScopeHandler>> results, ObjectScopeHandler[] nResults, int start, int index, int n) {
        int size = scopeHandlers.size();
        if (index < n) {
            for (int i = start; (i < size) && ((size-i) >= (n-index)); i++) {
                nResults[index] = (scopeHandlers.get(i));
                getAllNCombination(scopeHandlers, results, nResults, i+1, index+1, n);
            }
        } else {
            results.add(new ArrayList<>(Arrays.asList(nResults)));
        }
    }

}
