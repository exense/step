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

import com.mongodb.Block;

import step.core.Version;
import step.migration.MigrationTask;

/**
 * This task migrates the screen inputs from screen 'functionTable'
 * which require the prefix "attributes." as of 3.11
 *
 */
public class ScreenTemplateMigrationTask extends MigrationTask {

	public ScreenTemplateMigrationTask() {
		super(new Version(3,11,0));
	}

	@Override
	public void runUpgradeScript() {
		com.mongodb.client.MongoCollection<Document> screenInputs = context.getMongoClientSession().getMongoDatabase().getCollection("screenInputs");
		Document filter = new Document("screenId", "functionTable");
		screenInputs.find(filter).forEach(new Block<Document>() {
			@Override
			public void apply(Document t) {
				try {
					Document input = (Document) t.get("input");
					if(input.getString("id").equals("name")) {
						input.put("id", "attributes.name");
					}
					
					Document filter = new Document("_id", t.get("_id"));
					screenInputs.replaceOne(filter, t);
					logger.info("Migrating screen input to "+t.toJson());
				} catch(ClassCastException e) {
					// ignore
				}
			}
		});
	}
	
	@Override
	public void runDowngradeScript() {
		
	}

}
