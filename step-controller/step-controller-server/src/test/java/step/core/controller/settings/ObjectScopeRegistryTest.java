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

public class ObjectScopeRegistryTest {

    private ObjectScopeRegistry objectScopeRegistry;
    private AbstractScopedObjectAccessor<AbstractScopedObject> abstractScopeObjectInMemoryAccessor;

    @Before
    public void before() {
        objectScopeRegistry = new ObjectScopeRegistry();
        objectScopeRegistry.register("user", new ObjectScopeHandler("user") {
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



        objectScopeRegistry.register("scope1", new ObjectScopeHandler("scope1") {
            @Override
            protected String getScopeValue(Session<?> session) {
                return (String) session.get("scope1");
            }

            @Override
            public int getPriority() {
                return 50;
            }
        });

        objectScopeRegistry.register("scope2", new ObjectScopeHandler("scope2") {
            @Override
            protected String getScopeValue(Session<?> session) {
                return (String) session.get("scope2");
            }

            @Override
            public int getPriority() {
                return 10;
            }
        });

        abstractScopeObjectInMemoryAccessor = new AbstractScopedObjectAccessor<>(new InMemoryCollection<>(false), objectScopeRegistry);
    }

    @Test
    public void testRegistry() {
        //Create session with 2 scope info
        Session<User> session = new Session<>();
        User user = new User();
        user.setUsername("myUser");
        session.setUser(user);
        session.put("scope1","test1");

        Map<String, String> baseScope = Map.of("baseScopeKey", "baseScopeValue");

        //Create scope object
        AbstractScopedObject abstractScopeObjectFinal = new AbstractScopedObject();
        //Try to save object with unknown scope
        assertThrows("The scope 'project' is not registered", java.lang.UnsupportedOperationException.class,
                () -> abstractScopeObjectInMemoryAccessor.saveScopedObject(baseScope, abstractScopeObjectFinal, List.of("user", "project"), session));
        //Save settings with no scope info
        AbstractScopedObject abstractScopeObject = new AbstractScopedObject();
        ObjectId noScopeId = abstractScopeObject.getId();
        abstractScopeObjectInMemoryAccessor.saveScopedObject(baseScope, abstractScopeObject, List.of(), session);
        //Test with 2 scopes requested
        abstractScopeObject = new AbstractScopedObject();
        abstractScopeObjectInMemoryAccessor.saveScopedObject(baseScope, abstractScopeObject, List.of("user", "scope1"), session);
        ObjectId twoScopesId = abstractScopeObject.getId();

        //make sure objects were properly enriched with scope info
        AbstractScopedObject noScope = (AbstractScopedObject) abstractScopeObjectInMemoryAccessor.get(noScopeId);
        assertNull(noScope.getScope().get("user"));
        assertNull(noScope.getScope().get("scope1"));
        assertNull(noScope.getScope().get("scope2"));

        AbstractScopedObject twoScopes = (AbstractScopedObject) abstractScopeObjectInMemoryAccessor.get(twoScopesId);
        assertEquals("myUser", twoScopes.getScope().get("user"));
        assertEquals("test1", twoScopes.getScope().get("scope1"));
        assertNull(twoScopes.getScope().get("scope2"));

        //check the logic to retrieve a settings based on scope information in the session
        Optional<AbstractScopedObject> mySetting = abstractScopeObjectInMemoryAccessor.getScopedObject(baseScope, session);
        assertSettingResolution(mySetting, twoScopesId, Map.of("user", "myUser","scope1","test1"));

        //Save setting with 1st scope only, should overwrite more specific scope defined with same user and scope1
        abstractScopeObject = new AbstractScopedObject();
        abstractScopeObjectInMemoryAccessor.saveScopedObject(baseScope, abstractScopeObject, List.of("user"), session);
        ObjectId userScopeId = abstractScopeObject.getId();

        assertNull(abstractScopeObjectInMemoryAccessor.get(twoScopesId));

        //Save setting with 2nd scope only
        abstractScopeObject = new AbstractScopedObject();
        abstractScopeObjectInMemoryAccessor.saveScopedObject(baseScope, abstractScopeObject, List.of("scope1"), session);
        ObjectId scope1Id = abstractScopeObject.getId();

        //Assert settings saved for user scope only
        AbstractScopedObject userScope = (AbstractScopedObject) abstractScopeObjectInMemoryAccessor.get(userScopeId);
        assertEquals("myUser", userScope.getScope().get("user"));
        assertNull(userScope.getScope().get("scope1"));
        assertNull(userScope.getScope().get("scope2"));

        //Assert setting saved for scope1 only
        AbstractScopedObject scope1 = (AbstractScopedObject) abstractScopeObjectInMemoryAccessor.get(scope1Id);
        assertNull(noScope.getScope().get("user"));
        assertEquals("test1", scope1.getScope().get("scope1"));
        assertNull(scope1.getScope().get("scope2"));

        //Check orders of returned filter, first must have the higher priority
        List<Filter> filtersInPriorityOrder = objectScopeRegistry.getFiltersInPriorityOrder(session);
        assertEquals(4, filtersInPriorityOrder.size());
        assertFilters(filtersInPriorityOrder.get(0), Map.of("scope.scope1","test1","scope.user","myUser"), List.of("scope.scope2"));
        assertFilters(filtersInPriorityOrder.get(1), Map.of("scope.user","myUser"), List.of("scope.scope1", "scope.scope2"));
        assertFilters(filtersInPriorityOrder.get(2), Map.of("scope.scope1","test1"), List.of("scope.scope2", "scope.user"));
        assertFilters(filtersInPriorityOrder.get(3), Map.of(), List.of("scope.scope2", "scope.scope1", "scope.user"));

        //Check the logic to retrieve a settings based on scope information in the session, settings with both scopes was overridden when saving for scope user only (user=myUser)
        mySetting = abstractScopeObjectInMemoryAccessor.getScopedObject(baseScope, session);
        assertSettingResolution(mySetting, userScopeId, Map.of("user", "myUser"));

        //Test with different scope info in session (also with scope info for which there are no settings saved
        Optional<AbstractScopedObject> noScopeSetting = abstractScopeObjectInMemoryAccessor.getScopedObject(baseScope, new Session<>());
        assertSettingResolution(noScopeSetting, noScopeId, Map.of());

        //User scope
        Session<User> sessionWithUserScope = new Session<>();
        sessionWithUserScope.setUser(user);
        Optional<AbstractScopedObject> userScopeSetting = abstractScopeObjectInMemoryAccessor.getScopedObject(baseScope, sessionWithUserScope);
        assertSettingResolution(userScopeSetting, userScopeId, Map.of("user", "myUser"));

        Session<User> sessionWithScope1 = new Session<>();
        sessionWithScope1.put("scope1","test1");
        Optional<AbstractScopedObject> scope1Setting = abstractScopeObjectInMemoryAccessor.getScopedObject(baseScope, sessionWithScope1);
        assertSettingResolution(scope1Setting, scope1Id, Map.of("scope1", "test1"));

        //Test existing scope attribute with different values
        //Save setting with 2nd scope only new Session different scope1 value
        Session<User> sessionNewScope = new Session<>();
        sessionNewScope.put("scope1","test2");
        abstractScopeObject = new AbstractScopedObject();
        abstractScopeObjectInMemoryAccessor.saveScopedObject(baseScope, abstractScopeObject, List.of("scope1"), sessionNewScope);
        scope1Setting = abstractScopeObjectInMemoryAccessor.getScopedObject(baseScope, sessionNewScope);
        assertSettingResolution(scope1Setting, abstractScopeObject.getId(), Map.of("scope1", "test2"));
        assertEquals(4, abstractScopeObjectInMemoryAccessor.stream().count());

        //Finally save the setting system wide (i.e. with no scope), only this system should remain in the collections
        String finalId = abstractScopeObjectInMemoryAccessor.saveScopedObject(baseScope, abstractScopeObject, List.of(), sessionNewScope).getId().toHexString();
        assertEquals(1, abstractScopeObjectInMemoryAccessor.stream().count());
        AbstractScopedObject finalObject = (AbstractScopedObject) abstractScopeObjectInMemoryAccessor.stream().findFirst().get();
        assertEquals(1, finalObject.getScope().size());
        assertEquals("baseScopeValue", finalObject.getScope().get("baseScopeKey"));

        //update existing scoped object with different scopes, previous object should remain unchanged and a new entry should be created
        String newId = abstractScopeObjectInMemoryAccessor.saveScopedObject(baseScope, abstractScopeObject, List.of("scope1"), sessionNewScope).getId().toHexString();;
        assertEquals(2, abstractScopeObjectInMemoryAccessor.stream().count());
        assertNotEquals(finalId, newId);
        AbstractScopedObject abstractScopedObject = abstractScopeObjectInMemoryAccessor.get(newId);
        assertEquals("test2", abstractScopedObject.getScope().get("scope1"));

    }

    /**
     * Helper method to assert the resolution of the scope object based on the session
     * @param scopeSetting the resolved scope setting (optional)
     * @param scopeObjectId the expected id of the resolved object
     * @param scopes the expected scope content of the resolved object
     */
    private void assertSettingResolution(Optional<AbstractScopedObject> scopeSetting, ObjectId scopeObjectId, Map<String, String> scopes) {
        assertTrue(scopeSetting.isPresent());
        AbstractScopedObject userScopeSettingObject = scopeSetting.get();
        assertEquals(scopeObjectId, userScopeSettingObject.getId());
        assertEquals(scopes.size()+1, userScopeSettingObject.getScope().size());
        scopes.forEach((k,v) -> assertEquals(v, userScopeSettingObject.getScope().get(k)));
    }

    /**
     * Helper to assert that the built filter contains the correct list of equals filters and "not exists" filters
     * @param filter the built filters to be checked
     * @param equalsFilters the expected list of equals filters
     * @param notExistingFilters the expected list of not exists filters
     */
    private void assertFilters(Filter filter, Map<String, String> equalsFilters, List<String> notExistingFilters) {
        assertTrue(filter instanceof And);
        And andFilter = (And) filter;
        equalsFilters.forEach((k,v) -> assertTrue(andFilter.getChildren().contains(Filters.equals(k,v))));
        notExistingFilters.forEach(v -> assertTrue(andFilter.getChildren().contains(Filters.not(Filters.exists(v)))));
    }
}
