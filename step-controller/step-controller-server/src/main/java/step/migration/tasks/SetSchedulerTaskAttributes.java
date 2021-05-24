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

import java.util.concurrent.atomic.AtomicInteger;

import step.core.Version;
import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.CollectionFactory;
import ch.exense.commons.core.collections.Document;
import ch.exense.commons.core.collections.DocumentObject;
import ch.exense.commons.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

public class SetSchedulerTaskAttributes extends MigrationTask {

	private final Collection<Document> tasksCollection;

	public SetSchedulerTaskAttributes(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 12, 1), collectionFactory, migrationContext);
		tasksCollection = collectionFactory.getCollection("tasks", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		logger.info("Searching for tasks with no attributes.name to be migrated...");

		AtomicInteger i = new AtomicInteger();
		tasksCollection.find(Filters.equals("attributes.name", (String) null), null, null, null, 0).forEach(t -> {
			i.incrementAndGet();
			((DocumentObject) t.computeIfAbsent("attributes", k -> new DocumentObject())).put(AbstractOrganizableObject.NAME,
					t.get("name"));
			
			tasksCollection.save(t);
		});

		logger.info("Migrated " + i.get() + " tasks.");

	}

	@Override
	public void runDowngradeScript() {
		// TODO Auto-generated method stub
	}

}
