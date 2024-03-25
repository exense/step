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
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.framework.server.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SettingScopeRegistry {

    private final Map<String, SettingScopeHandler> settingScopeHandlers = new ConcurrentHashMap<>();

    public void register(String scope, SettingScopeHandler settingScopeHandler) {
        settingScopeHandlers.put(scope, settingScopeHandler);
    }

    public void addScopes(AbstractScopeObject abstractScopeObject, List<String> scopes, Session<User> session) {
        Objects.requireNonNull(scopes, "Scope list cannot be null, use an empty list instead");
        scopes.forEach(s -> {
            SettingScopeHandler settingScopeHandler = settingScopeHandlers.get(s);
            if (settingScopeHandler != null) {
                settingScopeHandler.saveScopeDetails(abstractScopeObject, session);
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
    public List<Filter> buildFilters(List<String> scopes, Session session) {
        Objects.requireNonNull(scopes, "Scope list cannot be null, use an empty list instead");
        List<Filter> filters = new ArrayList<>();
        settingScopeHandlers.forEach((k,v) -> {
            if (scopes.contains(k)) {
                filters.add(v.getFilter(session));
            } else {
                filters.add(Filters.not(Filters.exists(v.getFilterField())));
            }
        });
        return filters;
    }

    /**
     * This method return the list of filters to be used to retrieve settings by scope
     * The list is in priority order, the last filter search for the entry with no scope information (application scope)
     * @param session the Step HTTP session from which the scope is extracted
     * @return the list of ordered filters to retrieve the settings
     */
    protected List<Filter> getFiltersInPriorityOrder(Session<User> session) {
        //Get available scope information in Session and retrieve applicable handlers
        List<Filter> filters = new ArrayList<>();
        List<SettingScopeHandler> scopeHandlers = settingScopeHandlers.values().stream()
                .filter(settingScopeHandler -> settingScopeHandler.scopeIsApplicable(session)).collect(Collectors.toList());

        //List all scope combinations, sort them from highest to lower priorities and return corresponding filters
        filters.addAll(getAllCombination(scopeHandlers).stream().sorted(new Comparator<List<SettingScopeHandler>>() {
            @Override
            public int compare(List<SettingScopeHandler> o1, List<SettingScopeHandler> o2) {
                Integer o1AggPriorities = o1.stream().map(SettingScopeHandler::getPriority).reduce(0, Integer::sum);
                Integer o2AggPriorities = o2.stream().map(SettingScopeHandler::getPriority).reduce(0, Integer::sum);

                return o2AggPriorities - o1AggPriorities;
            }
        }).map(l -> Filters.and(buildFilters(l.stream().map(h->h.getScopeName()).collect(Collectors.toList()), session)))
                .collect(Collectors.toList()));

        //Final filter is the empty filter to search for application scope
        filters.add(Filters.and(buildFilters(List.of(), session)));
        return filters;
    }

    public List<List<SettingScopeHandler>> getAllCombination(List<SettingScopeHandler> scopeHandlers) {
        int size = scopeHandlers.size();
        List<List<SettingScopeHandler>> results = new ArrayList<>();

        for (int i = size; i > 0; i--) {
            SettingScopeHandler[] nResults = new SettingScopeHandler[i];
            getAllNCombination(scopeHandlers, results, nResults, 0, 0, i);
        }
        return results;
    }

    public void getAllNCombination(List<SettingScopeHandler> scopeHandlers, List<List<SettingScopeHandler>> results, SettingScopeHandler[] nResults, int start, int index, int n) {
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
