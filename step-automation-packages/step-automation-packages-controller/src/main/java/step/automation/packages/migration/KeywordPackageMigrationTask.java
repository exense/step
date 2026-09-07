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

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;



/**
 * Moves the keyword packages out of the way, ahead of their replacement by automation packages in
 * Step 31.
 *
 * <p><b>This task deliberately does nothing else.</b> It renames the {@code functionPackage}
 * collection to the staging collection. The cleanup or redeployment as automation package belongs to
 * {@link KeywordPackageMigrationPlugin}, which runs later in the startup with the managers available.
 *
 * <p><b>Why it must stay this narrow.</b> Migration tasks are not run only at startup: imports apply them
 * as well on temporary collections, thus old export containing function packages would see their keywords deleted
 * or tried to be converted as AP depending on the configured mode.
 * Besides migration task must remain agnostic of managers and only rely on documents using model available at the targeted version
 */
public class KeywordPackageMigrationTask extends MigrationTask {

    /** The collection being retired, from the former {@code FunctionPackageEntity.entityName}. */
    static final String FUNCTION_PACKAGE_COLLECTION = "functionPackage";

    /**
     * Holds the keyword packages between this task and {@link KeywordPackageMigrationPlugin}, which
     * replaces them. Written only by this migration, and dropped once it has completed.
     */
    static final String STAGING_COLLECTION = "keywordPackageMigrationStaging";

    private final Collection<Document> keywordPackages;

    public KeywordPackageMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3, 31, 0), collectionFactory, migrationContext);

        this.keywordPackages = collectionFactory.getCollection(FUNCTION_PACKAGE_COLLECTION, Document.class);
    }

    @Override
    public void runUpgradeScript() {
        long count = keywordPackages.count(Filters.empty(), null);
        if (count == 0) {
            // Renaming a collection that doesn't exist would throw and calling getCollection
            // automatically creates the table for some factory, so we immediately drop it again
            logger.info("No keyword package found, dropping the collection.");
            keywordPackages.drop();
            return;
        }

        // Rename the collection for the actual migration/cleanup
        keywordPackages.rename(STAGING_COLLECTION);

        logger.info("Moved {} keyword package(s) aside for the migration to automation packages.", count);
    }
}
