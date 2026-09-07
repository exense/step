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
package step.automation.packages.migration;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.entities.EntityConstants;
import step.functions.Function;
import step.migration.MigrationContext;
import step.plugins.java.GeneralScriptFunction;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.util.Map;

import static org.junit.Assert.*;
import static step.automation.packages.migration.KeywordPackageMigrationExecutor.FUNCTION_PACKAGE_ID_CUSTOM_FIELD;
import static step.automation.packages.migration.KeywordPackageMigrationTask.FUNCTION_PACKAGE_COLLECTION;
import static step.automation.packages.migration.KeywordPackageMigrationTask.STAGING_COLLECTION;

/**
 * The migration task must do one thing and nothing else: rename the keyword package collection to the
 * staging collection, leaving every other collection alone.
 * <p>
 * The collections are looked up per call rather than held in fields, because a rename leaves any
 * handle taken beforehand pointing at the old name.
 */
public class KeywordPackageMigrationTaskTest {

    private CollectionFactory collectionFactory;

    @Before
    public void setUp() {
        collectionFactory = new InMemoryCollectionFactory(null);
    }

    /**
     * The regression guard for the whole design. Migration tasks also run on the import path, against
     * the temporary collections an archive was loaded into, using the same MigrationManager singleton
     * — so a task holding a live manager would write into the production database while migrating an
     * import. If this test ever needs a binding, that property has been lost.
     */
    @Test
    public void theTaskRunsWithNoBindingsAtAll() {
        keywordPackages().save(keywordPackage("payments", "resource:65120000000000000000000a"));

        new KeywordPackageMigrationTask(collectionFactory, new MigrationContext()).runUpgradeScript();

        assertEquals(0, count(keywordPackages()));
        assertEquals(1, count(staging()));
    }

    @Test
    public void everyPackageIsMovedToStagingKeepingItsIdAndContent() {
        StagedKeywordPackage original = keywordPackage("payments", "resource:65120000000000000000000a");
        original.setPackageLibrariesLocation("resource:65120000000000000000000b");
        original.setPackageAttributes(Map.of("team", "billing"));
        original.setTokenSelectionCriteria(Map.of("os", "linux"));
        original.setExecuteLocally(true);
        original = keywordPackages().save(original);

        runTask();

        assertEquals("the keyword package collection must be emptied", 0, count(keywordPackages()));
        StagedKeywordPackage staged = staging().find(Filters.id(original.getId()), null, null, null, 0)
                .findFirst().orElse(null);
        assertNotNull("the id is preserved: the plugin needs it to find the keywords", staged);
        assertEquals("payments", staged.getAttribute(AbstractOrganizableObject.NAME));
        assertEquals("resource:65120000000000000000000a", staged.getPackageLocation());
        assertEquals("resource:65120000000000000000000b", staged.getPackageLibrariesLocation());
        assertEquals(Map.of("team", "billing"), staged.getPackageAttributes());
        assertEquals(Map.of("os", "linux"), staged.getTokenSelectionCriteria());
        assertTrue(staged.isExecuteLocally());
    }

    @Test
    public void allPackagesAreMovedRegardlessOfWhatTheyLookLike() {
        keywordPackages().save(keywordPackage("resource-backed", "resource:65120000000000000000000a"));
        keywordPackages().save(keywordPackage("path-located", "/opt/step/keywords/payments.jar"));
        StagedKeywordPackage embedded = keywordPackage("embedded", "/opt/step/kw/embedded.jar");
        embedded.addCustomField(KeywordPackageClassifier.EMBEDDED_PACKAGE_CUSTOM_FIELD, "embedded.jar");
        keywordPackages().save(embedded);

        runTask();

        assertEquals(0, count(keywordPackages()));
        assertEquals("classification belongs to the plugin, so the task moves all of them",
                3, count(staging()));
    }

    /**
     * The keywords must survive the task untouched. On the import path the plugin never runs, so this
     * is what makes an imported archive keep its keywords as ordinary standalone keywords rather than
     * losing them.
     */
    @Test
    public void keywordsAndResourcesAreLeftExactlyAsTheyWere() {
        StagedKeywordPackage keywordPackage = keywordPackages().save(
                keywordPackage("payments", "resource:65120000000000000000000a"));
        ObjectId keywordId = keywords().save(keyword(keywordPackage.getId())).getId();
        ObjectId resourceId = resources().save(resource()).getId();

        runTask();

        Function keyword = keywords().find(Filters.id(keywordId), null, null, null, 0).findFirst().orElse(null);
        assertNotNull("the keyword must not be deleted", keyword);
        assertEquals("its reference must not be stripped either", keywordPackage.getId().toString(),
                keyword.getCustomField(FUNCTION_PACKAGE_ID_CUSTOM_FIELD));

        Resource resource = resources().find(Filters.id(resourceId), null, null, null, 0).findFirst().orElse(null);
        assertEquals("re-typing belongs to the plugin: doing it here would break an import",
                ResourceManager.RESOURCE_TYPE_FUNCTIONS, resource.getResourceType());
    }

    /**
     * The rename is skipped when there is nothing to move, because renaming a collection that does not
     * exist fails outright — MongoDB raises NamespaceNotFound, PostgreSQL rejects the ALTER TABLE.
     * <p>
     * The collection is dropped rather than left alone, which matters on PostgreSQL: asking the
     * factory for a collection runs CREATE TABLE, so this task's own constructor creates the table on
     * an installation that never had keyword packages. Without the drop the migration would be
     * creating the very table Step 31 removes.
     * <p>
     * The in-memory factory can reproduce neither a missing collection nor a real DROP, so this pins
     * the guard and the emptiness rather than the driver behaviour.
     */
    @Test
    public void anEmptyKeywordPackageCollectionIsDroppedRatherThanRenamed() {
        runTask();

        assertEquals(0, count(keywordPackages()));
        assertEquals("nothing may be staged when there was nothing to move", 0, count(staging()));
    }

    // --------------------------------------------------------------- helpers

    private void runTask() {
        new KeywordPackageMigrationTask(collectionFactory, new MigrationContext()).runUpgradeScript();
    }

    private Collection<StagedKeywordPackage> keywordPackages() {
        return collectionFactory.getCollection(FUNCTION_PACKAGE_COLLECTION, StagedKeywordPackage.class);
    }

    private Collection<StagedKeywordPackage> staging() {
        return collectionFactory.getCollection(STAGING_COLLECTION, StagedKeywordPackage.class);
    }

    private Collection<Function> keywords() {
        return collectionFactory.getCollection(EntityConstants.functions, Function.class);
    }

    private Collection<Resource> resources() {
        return collectionFactory.getCollection(EntityConstants.resources, Resource.class);
    }

    private StagedKeywordPackage keywordPackage(String name, String packageLocation) {
        StagedKeywordPackage keywordPackage = new StagedKeywordPackage();
        keywordPackage.addAttribute(AbstractOrganizableObject.NAME, name);
        keywordPackage.setPackageLocation(packageLocation);
        return keywordPackage;
    }

    private Function keyword(ObjectId keywordPackageId) {
        GeneralScriptFunction keyword = new GeneralScriptFunction();
        keyword.addAttribute(AbstractOrganizableObject.NAME, "Login");
        keyword.addCustomField(FUNCTION_PACKAGE_ID_CUSTOM_FIELD, keywordPackageId.toString());
        return keyword;
    }

    private Resource resource() {
        Resource resource = new Resource();
        resource.setResourceType(ResourceManager.RESOURCE_TYPE_FUNCTIONS);
        return resource;
    }

    private long count(Collection<?> collection) {
        return collection.find(Filters.empty(), null, null, null, 0).count();
    }
}
