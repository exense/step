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
package step.automation.packages.accessor;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import step.automation.packages.AutomationPackage;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * The layered accessor exists for the {@code apResource:} resolver: during an isolated execution a
 * globally deployed keyword may be executed, and its {@code apResource:} reference names a package
 * that lives in the global layer, not the isolated one. So resolution has to see both layers, while
 * writes must stay in the isolated one.
 */
public class LayeredAutomationPackageAccessorTest {

    private InMemoryAutomationPackageAccessorImpl isolated;
    private InMemoryAutomationPackageAccessorImpl global;

    private AutomationPackage isolatedPackage;
    private AutomationPackage globalPackage;

    private LayeredAutomationPackageAccessor accessor;

    @Before
    public void setUp() {
        isolated = new InMemoryAutomationPackageAccessorImpl();
        global = new InMemoryAutomationPackageAccessorImpl();

        isolatedPackage = save(isolated, "resource:isolatedArchive", "resource:isolatedLibrary");
        globalPackage = save(global, "resource:globalArchive", "resource:globalLibrary");

        accessor = new LayeredAutomationPackageAccessor(List.of(isolated, global));
    }

    private static AutomationPackage save(AutomationPackageAccessor accessor, String archiveResource,
                                          String libraryResource) {
        AutomationPackage automationPackage = new AutomationPackage();
        automationPackage.setAutomationPackageResource(archiveResource);
        automationPackage.setAutomationPackageLibraryResource(libraryResource);
        return accessor.save(automationPackage);
    }

    /**
     * The reason the class exists: an {@code apResource:<apId>:} reference must resolve whichever
     * layer its package was deployed to.
     */
    @Test
    public void resolvesAPackageOfEitherLayerById() {
        assertNotNull(accessor.get(isolatedPackage.getId()));
        assertNotNull(accessor.get(globalPackage.getId()));
        assertNull(accessor.get(new ObjectId()));
    }

    @Test
    public void findsByAutomationPackageResourceInEitherLayer() {
        assertEquals(List.of(isolatedPackage.getId()), ids(accessor.findByAutomationPackageResource("resource:isolatedArchive")));
        assertEquals(List.of(globalPackage.getId()), ids(accessor.findByAutomationPackageResource("resource:globalArchive")));
        assertEquals(List.of(), ids(accessor.findByAutomationPackageResource("resource:unknown")));
    }

    @Test
    public void findsByLibraryResourceInEitherLayer() {
        assertEquals(List.of(isolatedPackage.getId()), ids(accessor.findByLibraryResource("resource:isolatedLibrary")));
        assertEquals(List.of(globalPackage.getId()), ids(accessor.findByLibraryResource("resource:globalLibrary")));
        assertEquals(List.of(), ids(accessor.findByLibraryResource("resource:unknown")));
    }

    /**
     * Both finders merge the layers rather than stopping at the first hit, isolated layer first. No
     * deduplication: two packages sharing a resource are two distinct results.
     */
    @Test
    public void mergesTheLayersIsolatedFirst() {
        AutomationPackage isolatedShared = save(isolated, "resource:sharedArchive", "resource:sharedLibrary");
        AutomationPackage globalShared = save(global, "resource:sharedArchive", "resource:sharedLibrary");

        assertEquals(List.of(isolatedShared.getId(), globalShared.getId()),
            ids(accessor.findByAutomationPackageResource("resource:sharedArchive")));
        assertEquals(List.of(isolatedShared.getId(), globalShared.getId()),
            ids(accessor.findByLibraryResource("resource:sharedLibrary")));
    }

    /**
     * A write during an isolated execution must not reach the global accessor.
     */
    @Test
    public void savesIntoTheFirstLayerOnly() {
        AutomationPackage saved = save(accessor, "resource:newArchive", null);

        assertNotNull(isolated.get(saved.getId()));
        assertNull(global.get(saved.getId()));
    }

    @Test
    public void findsNothingWhenEmpty() {
        LayeredAutomationPackageAccessor empty =
            new LayeredAutomationPackageAccessor(List.of(new InMemoryAutomationPackageAccessorImpl()));

        assertEquals(List.of(), ids(empty.findByAutomationPackageResource("resource:isolatedArchive")));
        assertEquals(List.of(), ids(empty.findByLibraryResource("resource:sharedLibrary")));
    }

    private static List<ObjectId> ids(List<AutomationPackage> automationPackages) {
        return automationPackages.stream().map(AutomationPackage::getId).collect(Collectors.toList());
    }
}
