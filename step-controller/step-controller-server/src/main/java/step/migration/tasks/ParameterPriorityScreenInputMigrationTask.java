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
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.List;

/**
 * This task removes the priority fron the parameterTable screen inputs
 *
 */
public class ParameterPriorityScreenInputMigrationTask extends MigrationTask {

	private final Collection<Document> screenInputs;

	public ParameterPriorityScreenInputMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 21, 2), collectionFactory, migrationContext);
		screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		logger.info("Removing priority from the parameterTable screen input.");
		Filter priorityInputFilter = Filters.and(List.of(Filters.equals("screenId", "parameterTable"),
				Filters.equals("input.id", "priority")));
		screenInputs.remove(priorityInputFilter);
	}

	@Override
	public void runDowngradeScript() {

	}
}
