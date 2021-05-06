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

import org.bson.Document;

import com.mongodb.Block;

import step.core.Version;
import step.migration.MigrationTask;

/**
 * This task migrates the artefact of type 'CallFunction' that have the attribute 'function' declared as string instead of DynamicValue
 * do this only when migrating from 3.4.0 to 3.5.0 or higher
 *
 */
public class SetSchedulerTaskAttributes extends MigrationTask {

	public SetSchedulerTaskAttributes() {
		super(new Version(3,12,1));
	}

	@Override
	public void runUpgradeScript() {
		logger.info("Searching for tasks with no attributes.name to be migrated...");
		com.mongodb.client.MongoCollection<Document> tasks = mongoClientSession.getMongoDatabase().getCollection("tasks");
		
		AtomicInteger i = new AtomicInteger();
		Document filterTasksWithNoAttrName = new Document("attributes.name", null);
		tasks.find(filterTasksWithNoAttrName).forEach(new Block<Document>() {
			@Override
			public void apply(Document t) {
				try {
					i.incrementAndGet();
					Document filter = new Document("_id", t.get("_id"));
					Document update = new Document("$set", new Document("attributes.name",t.get("name")));
					tasks.updateOne(filter, update);
				} catch(ClassCastException e) {
					// ignore
				}
			}
		});
		
		logger.info("Migrated "+i.get()+" tasks.");
		
	}

	@Override
	public void runDowngradeScript() {
		// TODO Auto-generated method stub
		
	}


}
