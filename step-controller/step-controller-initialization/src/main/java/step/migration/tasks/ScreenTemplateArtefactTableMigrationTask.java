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

import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.Document;

import step.core.GlobalContext;
import step.core.Version;
import step.migration.MigrationTask;

/**
 * This task migrates the screen inputs from screen 'artefactTable' to 'planTable'
 *
 */
public class ScreenTemplateArtefactTableMigrationTask extends MigrationTask {

	private com.mongodb.client.MongoCollection<Document> screenInputs;

	public ScreenTemplateArtefactTableMigrationTask() {
		super(new Version(3,13,0));
	}

	@Override
	protected void setContext(GlobalContext context) {
		screenInputs = context.getMongoClientSession().getMongoDatabase().getCollection("screenInputs");
	}

	@Override
	public void runUpgradeScript() {
		Document filter = new Document("screenId", "artefactTable");
		screenInputs.find(filter).iterator().forEachRemaining(t -> {
			try {
				t.put("screenId", "planTable");
				Document input = (Document) t.get("input");
				
				String inputId = input.getString("id");
				if(inputId.equals("attributes.name")) {
					input.put("valueHtmlTemplate", "<plan-link plan-id=\"stBean.id\" description=\"stBean.attributes.name\" />");
				}
				
				Document idFilter = new Document("_id", t.get("_id"));
				if(!inputExist(inputId)) {
					screenInputs.replaceOne(idFilter, t);
					logger.info("Migrating screen input to "+t.toJson());
				} else {
					screenInputs.deleteOne(idFilter);
					logger.info("Deleted screen input "+t.toJson());
				}
			} catch(ClassCastException e) {
				// ignore
			}
		});
	}

	protected boolean inputExist(String inputId) {
		Document planFilter = new Document("screenId", "planTable");
		AtomicBoolean result = new AtomicBoolean();
		screenInputs.find(planFilter).iterator().forEachRemaining(doc -> {
			Document i = (Document) doc.get("input");
			if(i.getString("id").equals(inputId)) {
				result.set(true);
			}
		});
		return result.get();
	}
	
	@Override
	public void runDowngradeScript() {
		
	}

}
