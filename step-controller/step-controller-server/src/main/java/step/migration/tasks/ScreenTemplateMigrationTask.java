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
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.CollectionFactory;
import ch.exense.commons.core.collections.Document;
import ch.exense.commons.core.collections.DocumentObject;
import ch.exense.commons.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

/**
 * This task migrates the screen inputs from screen 'functionTable' which
 * require the prefix "attributes." as of 3.11
 *
 */
public class ScreenTemplateMigrationTask extends MigrationTask {

	private final Collection<Document> screenInputs;

	public ScreenTemplateMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 11, 0), collectionFactory, migrationContext);
		screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		screenInputs.find(Filters.equals("screenId", "functionTable"), null, null, null, 0).forEach(t -> {

			DocumentObject input = t.getObject("input");
			if (input.getString("id").equals("name")) {
				input.put("id", "attributes.name");
			}

			screenInputs.save(t);
		});
	}

	@Override
	public void runDowngradeScript() {

	}
}
