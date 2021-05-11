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
import step.core.collections.Filters.Equals;
import step.migration.MigrationTask;

/**
 * This task removes the functions of type 'LocalFunction'
 *
 */
public class RemoveLocalFunctions extends MigrationTask {

	private final Collection<Document> functions;

	public RemoveLocalFunctions(CollectionFactory collectionFactory) {
		super(new Version(3,13,0), collectionFactory);
		functions = collectionFactory.getCollection("functions", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		removeLocalFunctions();
	}
	
	private void removeLocalFunctions() {
		logger.info("Searching for keywords of type 'LocalFunction' to be deleted...");
		
		Equals filter = Filters.equals("type", "step.functions.base.types.LocalFunction");
		long count = functions.find(filter, null, null, null, 0).count();
		functions.remove(filter);
		
		logger.info("Removed "+count+" keywords of type 'LocalFunction'");
	}
		
	@Override
	public void runDowngradeScript() {
		
	}

}
