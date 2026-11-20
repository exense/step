/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
package step.plugins.remote;

import org.junit.Before;
import org.junit.Test;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.CachedAccessor;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.entities.Entity;
import step.core.entities.EntityManager;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RemoteCollectionServicesTest {

    private static final String CACHED_ENTITY = "cachedEntity";

    private RemoteCollectionServices<AbstractOrganizableObject> services;
    private CachedAccessor<AbstractOrganizableObject> cachedAccessor;

    @Before
    public void before() {
        InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(new Properties());
        cachedAccessor = new CachedAccessor<>(new AbstractAccessor<>(collectionFactory.getCollection(CACHED_ENTITY, AbstractOrganizableObject.class)));
        EntityManager entityManager = new EntityManager();
        entityManager.register(new Entity<>(CACHED_ENTITY, cachedAccessor, AbstractOrganizableObject.class));

        services = new RemoteCollectionServices<>();
        services.collectionFactory = collectionFactory;
        services.entityManager = entityManager;
    }

    @Test
    public void cachedAccessorReloadedAfterWrites() {
        AbstractOrganizableObject entity1 = entity("entity1");
        services.save(CACHED_ENTITY, entity1);
        assertEquals("entity1", cachedAccessor.get(entity1.getId()).getAttribute(AbstractOrganizableObject.NAME));

        AbstractOrganizableObject entity2 = entity("entity2");
        AbstractOrganizableObject entity3 = entity("entity3");
        services.saveBulk(CACHED_ENTITY, List.of(entity2, entity3));
        assertEquals(3, cachedAccessor.stream().count());

        services.delete(CACHED_ENTITY, Filters.id(entity2.getId()));
        assertNull(cachedAccessor.get(entity2.getId()));
        assertEquals(2, cachedAccessor.stream().count());
    }

    private static AbstractOrganizableObject entity(String name) {
        AbstractOrganizableObject entity = new AbstractOrganizableObject();
        entity.addAttribute(AbstractOrganizableObject.NAME, name);
        return entity;
    }
}
