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
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.Filters.Equals;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

/**
 * This task removes the functions of type 'LocalFunction'
 *
 */
public class MigrateSeleniumFunctions extends MigrationTask {

	private final Collection<Document> functions;

	public MigrateSeleniumFunctions(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,17,0), collectionFactory, migrationContext);
		functions = collectionFactory.getCollection("functions", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		logger.info("Searching for keywords of type 'SeleniumFunction' to be migrated...");
		
		AtomicInteger count = new AtomicInteger();
		Equals filter = Filters.equals("type", "step.plugins.selenium.SeleniumFunction");
		functions.find(filter, null, null, null, 0).forEach(f -> {
			f.put("type", "step.plugins.java.GeneralScriptFunction");
			f.remove("seleniumVersion");
			functions.save(f);
			count.incrementAndGet();
		});
		
		logger.info("Migrated "+count.get()+" keywords of type 'SeleniumFunction'");
	}
		
	@Override
	public void runDowngradeScript() {
		
	}

}
