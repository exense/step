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
import org.junit.Before;
import org.junit.Test;
import step.core.access.User;
import step.core.accessors.AbstractUser;
import step.core.collections.Filters;
import step.core.collections.Filter;
import step.core.collections.filters.And;
import step.core.collections.inmemory.InMemoryCollection;
import step.framework.server.Session;

import java.util.*;

import static org.junit.Assert.*;

public class SettingScopeRegistryTest {

    private SettingScopeRegistry settingScopeRegistry;
    private AbstractScopeAccessor<AbstractScopeObject> abstractScopeObjectInMemoryAccessor;

    @Before
    public void before() {
        settingScopeRegistry = new SettingScopeRegistry();
        settingScopeRegistry.register("user", new SettingScopeHandler("user") {
            @Override
            protected String getScopeValue(Session<?> session) {
                AbstractUser user = session.getUser();
                return (user != null) ? user.getSessionUsername() : null;
            }

            @Override
            public int getPriority() {
                return 100;
            }

        });



        settingScopeRegistry.register("scope1", new SettingScopeHandler("scope1") {
            @Override
            protected String getScopeValue(Session<?> session) {
                return (String) session.get("scope1");
            }

            @Override
            public int getPriority() {
                return 50;
            }
        });

        settingScopeRegistry.register("scope2", new SettingScopeHandler("scope2") {
            @Override
            protected String getScopeValue(Session<?> session) {
                return (String) session.get("scope2");
            }

            @Override
            public int getPriority() {
                return 10;
            }
        });

        abstractScopeObjectInMemoryAccessor = new AbstractScopeAccessor<>(new InMemoryCollection<>(false), settingScopeRegistry);
    }

    @Test
    public void testRegistry() {
        //Create session with 2 scope info
        Session<User> session = new Session<>();
        User user = new User();
        user.setUsername("myUser");
        session.setUser(user);
        session.put("scope1","test1");

        //Create scope object
        AbstractScopeObject abstractScopeObjectFinal = new AbstractScopeObject();
        abstractScopeObjectFinal.setSettingId("mySetting");
        //Try to save object with unknown scope
        assertThrows("The scope 'project' is not registered", java.lang.UnsupportedOperationException.class,
                () -> abstractScopeObjectInMemoryAccessor.saveSetting(abstractScopeObjectFinal, List.of("user", "project"), session));
        //Save settings with no scope info
        AbstractScopeObject abstractScopeObject = new AbstractScopeObject();
        abstractScopeObject.setSettingId("mySetting");
        ObjectId noScopeId = abstractScopeObject.getId();
        abstractScopeObjectInMemoryAccessor.saveSetting(abstractScopeObject, List.of(), session);
        //Test with 2 scopes requested
        abstractScopeObject = new AbstractScopeObject();
        abstractScopeObject.setSettingId("mySetting");
        abstractScopeObjectInMemoryAccessor.saveSetting(abstractScopeObject, List.of("user", "scope1"), session);
        ObjectId twoScopesId = abstractScopeObject.getId();
        //Save setting with 1st scope only
        abstractScopeObject = new AbstractScopeObject();
        abstractScopeObject.setSettingId("mySetting");
        abstractScopeObjectInMemoryAccessor.saveSetting(abstractScopeObject, List.of("user"), session);
        ObjectId userScopeId = abstractScopeObject.getId();
        //Save setting with 2nd scope only
        abstractScopeObject = new AbstractScopeObject();
        abstractScopeObject.setSettingId("mySetting");
        abstractScopeObjectInMemoryAccessor.saveSetting(abstractScopeObject, List.of("scope1"), session);
        ObjectId scope1Id = abstractScopeObject.getId();

        //make sure objects were properly enriched with scope info
        AbstractScopeObject noScope = (AbstractScopeObject) abstractScopeObjectInMemoryAccessor.get(noScopeId);
        assertNull(noScope.getScope().get("user"));
        assertNull(noScope.getScope().get("scope1"));
        assertNull(noScope.getScope().get("scope2"));

        AbstractScopeObject twoScopes = (AbstractScopeObject) abstractScopeObjectInMemoryAccessor.get(twoScopesId);
        assertEquals("myUser", twoScopes.getScope().get("user"));
        assertEquals("test1", twoScopes.getScope().get("scope1"));
        assertNull(twoScopes.getScope().get("scope2"));

        AbstractScopeObject userScope = (AbstractScopeObject) abstractScopeObjectInMemoryAccessor.get(userScopeId);
        assertEquals("myUser", userScope.getScope().get("user"));
        assertNull(userScope.getScope().get("scope1"));
        assertNull(userScope.getScope().get("scope2"));

        AbstractScopeObject scope1 = (AbstractScopeObject) abstractScopeObjectInMemoryAccessor.get(scope1Id);
        assertNull(noScope.getScope().get("user"));
        assertEquals("test1", scope1.getScope().get("scope1"));
        assertNull(scope1.getScope().get("scope2"));

        //Check orders of returned filter, first must have the higher priority
        List<Filter> filtersInPriorityOrder = settingScopeRegistry.getFiltersInPriorityOrder(session);
        assertEquals(4, filtersInPriorityOrder.size());
        assertFilters(filtersInPriorityOrder.get(0), Map.of("scope.scope1","test1","scope.user","myUser"), List.of("scope.scope2"));
        assertFilters(filtersInPriorityOrder.get(1), Map.of("scope.user","myUser"), List.of("scope.scope1", "scope.scope2"));
        assertFilters(filtersInPriorityOrder.get(2), Map.of("scope.scope1","test1"), List.of("scope.scope2", "scope.user"));
        assertFilters(filtersInPriorityOrder.get(3), Map.of(), List.of("scope.scope2", "scope.scope1", "scope.user"));

        //Final check the logic to retrieve a settings based on scope information in the session
        Optional<AbstractScopeObject> mySetting = abstractScopeObjectInMemoryAccessor.getSetting("mySetting", session);
        assertSettingResolution(mySetting, twoScopesId, Map.of("user", "myUser","scope1","test1"));

        //Test with different scope info in session (also with scope info for which there are no settings saved
        Optional<AbstractScopeObject> noScopeSetting = abstractScopeObjectInMemoryAccessor.getSetting("mySetting", new Session<>());
        assertSettingResolution(noScopeSetting, noScopeId, Map.of());

        //User scope
        Session<User> sessionWithUserScope = new Session<>();
        sessionWithUserScope.setUser(user);
        Optional<AbstractScopeObject> userScopeSetting = abstractScopeObjectInMemoryAccessor.getSetting("mySetting", sessionWithUserScope);
        assertSettingResolution(userScopeSetting, userScopeId, Map.of("user", "myUser"));

        Session<User> sessionWithScope1 = new Session<>();
        sessionWithScope1.put("scope1","test1");
        Optional<AbstractScopeObject> scope1Setting = abstractScopeObjectInMemoryAccessor.getSetting("mySetting", sessionWithScope1);
        assertSettingResolution(scope1Setting, scope1Id, Map.of("scope1", "test1"));

        //Test existing scope attribute with different values
        //Save setting with 2nd scope only new Session different scope1 value
        Session<User> sessionNewScope = new Session<>();
        sessionNewScope.put("scope1","test2");
        abstractScopeObject = new AbstractScopeObject();
        abstractScopeObject.setSettingId("mySetting");
        abstractScopeObjectInMemoryAccessor.saveSetting(abstractScopeObject, List.of("scope1"), sessionNewScope);
        scope1Setting = abstractScopeObjectInMemoryAccessor.getSetting("mySetting", sessionNewScope);
        assertSettingResolution(scope1Setting, abstractScopeObject.getId(), Map.of("scope1", "test2"));
        assertEquals(5, abstractScopeObjectInMemoryAccessor.stream().count());
    }

    private void assertSettingResolution(Optional<AbstractScopeObject> scopeSetting, ObjectId scopeId, Map<String, String> scopes) {
        assertTrue(scopeSetting.isPresent());
        AbstractScopeObject userScopeSettingObject = scopeSetting.get();
        assertEquals(scopeId, userScopeSettingObject.getId());
        assertEquals(scopes.size(), userScopeSettingObject.getScope().size());
        scopes.forEach((k,v) -> assertEquals(v, userScopeSettingObject.getScope().get(k)));
    }

    private void assertFilters(Filter filter, Map<String, String> equalsFilters, List<String> notExistingFilters) {
        assertTrue(filter instanceof And);
        And andFilter = (And) filter;
        equalsFilters.forEach((k,v) -> assertTrue(andFilter.getChildren().contains(Filters.equals(k,v))));
        notExistingFilters.forEach(v -> assertTrue(andFilter.getChildren().contains(Filters.not(Filters.exists(v)))));
    }
}
