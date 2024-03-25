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
package step.migration.tasks;

import step.core.Version;
import step.core.collections.*;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

/**
 * This task migrates the screen inputs from screen 'functionTable' which
 * require the prefix "attributes." as of 3.11
 *
 */
public class ScreenTemplateMigrationTask25 extends MigrationTask {

	private final Collection<Document> screenInputs;

	public ScreenTemplateMigrationTask25(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 25, 0), collectionFactory, migrationContext);
		screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		logger.info("Cleaning up screen inputs which are replaced by the new table and columns configurations mechanism.");
		screenInputs.remove(Filters.equals("screenId", "executionTable"));
		screenInputs.remove(Filters.equals("screenId", "schedulerTable"));
		screenInputs.remove(Filters.equals("screenId", "parameterDialog"));
		screenInputs.remove(Filters.equals("screenId", "parameterTable"));
		screenInputs.remove(Filters.equals("screenId", "functionTableExtensions"));
	}

	@Override
	public void runDowngradeScript() {

	}
}
