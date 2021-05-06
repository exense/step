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

import org.bson.Document;

import com.mongodb.client.result.DeleteResult;

import step.core.Version;
import step.migration.MigrationTask;

/**
 * This task removes the functions of type 'LocalFunction'
 *
 */
public class RemoveLocalFunctions extends MigrationTask {

	public RemoveLocalFunctions() {
		super(new Version(3,13,0));
	}

	@Override
	public void runUpgradeScript() {
		removeLocalFunctions();
	}
	
	private void removeLocalFunctions() {
		logger.info("Searching for keywords of type 'LocalFunction' to be deleted...");
		
		com.mongodb.client.MongoCollection<Document> functions = mongoClientSession.getMongoDatabase().getCollection("functions");
		
		Document filter = new Document("type", "step.functions.base.types.LocalFunction");
		DeleteResult result = functions.deleteMany(filter);
		
		logger.info("Removed "+result.getDeletedCount()+" keywords of type 'LocalFunction'");
	}
		
	@Override
	public void runDowngradeScript() {
		
	}

}
