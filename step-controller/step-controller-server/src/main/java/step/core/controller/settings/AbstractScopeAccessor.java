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
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.framework.server.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class AbstractScopeAccessor<T extends AbstractScopeObject> extends AbstractAccessor {

    private final SettingScopeRegistry settingScopeRegistry;

    public AbstractScopeAccessor(Collection<T> collectionDriver, SettingScopeRegistry settingScopeRegistry) {
        super(collectionDriver);
        this.settingScopeRegistry = settingScopeRegistry;
    }

    /**
     * Save settings applying selected scope
     * @param setting the settings to be saved
     * @param scopes the scope to be applied
     * @param session the related session (used to retrieve scope values)
     */
    public void saveSetting(T setting, List<String> scopes, Session<User> session) {
        //Override previous scope in case of update
        setting.setScope(new HashMap<>());
        //Enrich with requested scope based on session context
        settingScopeRegistry.addScopes(setting, scopes, session);
        //Check if element exists for this scope, and set for update in this case
        //scope combination must be unique by setting id
        Optional<T> bySettingIdAndScope = getBySettingIdAndScope(setting.getSettingId(), scopes, session);
        bySettingIdAndScope.ifPresent(s -> setting.setId(s.getId()));
        //Finally save or update
        save(setting);
    }

    /**
     * Retrieve the setting value for settingId with the highest priority given the current session context.
     * Example: one setting is stored for scope project=projectA and user=userA and another one only with scope user=userA.
     * If the session is for userA in project A, the first setting will be returned, if the session is for userA in projectB,
     * the 2nd setting will be returned, otherwise no setting will be returned
     * @param settingId the setting ID to be retrieved
     * @param session the session used to match stored settings by scope
     * @return optional setting
     */
    public Optional<T> getSetting(String settingId, Session<User> session) {
        Optional<T> first = Optional.empty();
        for (Filter filter : settingScopeRegistry.getFiltersInPriorityOrder(session)) {
            first = this.collectionDriver.find(filter, null, null, null, 0).findFirst();
            if (first.isPresent()){
                break;
            }
        }
        return first;
    }

    private Optional<T> getBySettingIdAndScope(String settingId, List<String> scopes, Session<User> session) {
        List<Filter> scopeFilters = settingScopeRegistry.buildFilters(scopes, session);
        scopeFilters.add(Filters.equals("settingId", settingId));
        return this.collectionDriver.find((Filters.and(scopeFilters)), null, 0, 1, 0).findFirst();
    }
}
