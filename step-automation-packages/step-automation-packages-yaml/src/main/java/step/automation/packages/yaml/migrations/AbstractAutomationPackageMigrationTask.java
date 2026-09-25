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
package step.automation.packages.yaml.migrations;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

/**
 * Base class of the migrations applied to an automation package descriptor or fragment when it declares a schema
 * version older than the current one.
 * <p>
 * These migrations concern the body of the descriptor itself, for instance its parameters. The plans it contains
 * are migrated separately, by the migrations of the yaml plan format.
 */
public abstract class AbstractAutomationPackageMigrationTask extends MigrationTask {

    public static final String AUTOMATION_PACKAGE_DESCRIPTORS_COLLECTION_NAME = "automationPackageDescriptors";

    protected final Collection<Document> descriptorsCollection;

    public AbstractAutomationPackageMigrationTask(Version asOfVersion, CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(asOfVersion, collectionFactory, migrationContext);
        descriptorsCollection = collectionFactory.getCollection(AUTOMATION_PACKAGE_DESCRIPTORS_COLLECTION_NAME, Document.class);
    }

    @Override
    public void runDowngradeScript() {
    }
}
