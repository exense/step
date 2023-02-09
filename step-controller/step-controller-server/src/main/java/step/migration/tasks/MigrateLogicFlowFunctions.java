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
import step.core.collections.Filters;
import step.core.collections.filters.Equals;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This task migrates the functions of type 'LogicFlowFunction' to 'AstraFunction'
 *
 */
public class MigrateLogicFlowFunctions extends MigrationTask {

	private final Collection<Document> functions;

	public MigrateLogicFlowFunctions(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,21,0), collectionFactory, migrationContext);
		functions = collectionFactory.getCollection("functions", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		logger.info("Searching for keywords of type 'LogicFlowFunction' to be migrated...");
		
		AtomicInteger count = new AtomicInteger();
		Equals filter = Filters.equals("type", "step.plugins.logicflow.LogicFlowFunction");
		functions.find(filter, null, null, null, 0).forEach(f -> {
			f.put("type", "step.plugins.astra.AstraFunction");
			String testSuiteId = f.getString("testSuiteId");
			if(testSuiteId != null) {
				f.put("suiteId", testSuiteId);
			}
			functions.save(f);
			count.incrementAndGet();
		});
		
		logger.info("Migrated "+count.get()+" keywords of type 'LogicFlowFunction'");
	}
		
	@Override
	public void runDowngradeScript() {
		
	}

}
