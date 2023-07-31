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
package step.plans.parser.yaml.migrations;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.migration.MigrationContext;
import step.migration.MigrationTask;
import step.plans.parser.yaml.YamlPlanReader;

public abstract class AbstractYamlPlanMigrationTask extends MigrationTask {

    protected final Collection<Document> yamlPlansCollection;

    public AbstractYamlPlanMigrationTask(Version asOfVersion, CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(asOfVersion, collectionFactory, migrationContext);
        yamlPlansCollection = collectionFactory.getCollection(YamlPlanReader.YAML_PLANS_COLLECTION_NAME, Document.class);
    }

    @Override
    public void runDowngradeScript() {
        throw new UnsupportedOperationException("Simple plan downgrading is not supported");
    }
}
