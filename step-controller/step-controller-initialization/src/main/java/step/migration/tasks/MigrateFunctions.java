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
import com.mongodb.client.result.UpdateResult;

import step.core.GlobalContext;
import step.core.Version;
import step.migration.MigrationTask;

/**
 * This task migrates the artefact of type 'CallFunction' that have the attribute 'function' declared as string instead of DynamicValue
 * do this only when migrating from 3.4.0 to 3.5.0 or higher
 *
 */
public class MigrateFunctions extends MigrationTask {

	public MigrateFunctions() {
		super(new Version(3,5,0));
	}

	@Override
	public void runUpgradeScript() {
		migrateCallFunction(context);
		migrateGeneralScriptFunction(context);
		migrateGeneralScriptFunctions(context);
	}
	
	private void migrateGeneralScriptFunction(GlobalContext context) {
		logger.info("Searching for keywords of type 'Script' to be migrated...");
		
		com.mongodb.client.MongoCollection<Document> functions = mongoClientSession.getMongoDatabase().getCollection("functions");
		
		Document filter = new Document("type", "step.plugins.functions.types.GeneralScriptFunction");
		Document replacement = new Document("$set", new Document("type", "step.plugins.java.GeneralScriptFunction"));
		UpdateResult result = functions.updateMany(filter, replacement);
		
		logger.info("Migrated "+result.getModifiedCount()+" artefacts of type 'step.plugins.functions.types.GeneralScriptFunction'");
	}
	
	private void migrateGeneralScriptFunctions(GlobalContext context) {
		logger.info("Searching for functions of type 'step.plugins.functions.types.GeneralScriptFunction' to be migrated...");
		com.mongodb.client.MongoCollection<Document> functions = mongoClientSession.getMongoDatabase().getCollection("functions");
		
		AtomicInteger i = new AtomicInteger();
		Document filterCallFunction = new Document("type", "step.plugins.functions.types.GeneralScriptFunction");
		functions.find(filterCallFunction).forEach(new Block<Document>() {

			@Override
			public void apply(Document t) {
				t.replace("type", "step.plugins.java.GeneralScriptFunction");
				Document filter = new Document("_id", t.get("_id"));
				functions.replaceOne(filter, t);
				i.incrementAndGet();
			}
		});
		
		logger.info("Migrated "+i.get()+" functions of type 'step.plugins.functions.types.GeneralScriptFunction'");
	}
	
	// This function migrates the artefact of type 'CallFunction' that have the attribute 'function' declared as string instead of DynamicValue
	// TODO do this only when migrating from 3.4.0 to 3.5.0 or higher
	private void migrateCallFunction(GlobalContext context) {
		logger.info("Searching for artefacts of type 'CallFunction' to be migrated...");
		com.mongodb.client.MongoCollection<Document> artefacts = mongoClientSession.getMongoDatabase().getCollection("artefacts");
		
		AtomicInteger i = new AtomicInteger();
		Document filterCallFunction = new Document("_class", "CallFunction");
		artefacts.find(filterCallFunction).forEach(new Block<Document>() {

			@Override
			public void apply(Document t) {
				if(t.containsKey("function")) {
					try {
						i.incrementAndGet();
						String function = t.getString("function");
						Document d = new Document();
						d.append("dynamic", false);
						d.append("value", function);
						t.replace("function", d);
						
						Document filter = new Document("_id", t.get("_id"));
						
						artefacts.replaceOne(filter, t);
						logger.info("Migrating "+function+" to "+d.toJson());
					} catch(ClassCastException e) {
						// ignore
					}
				}
			}
		});
		
		logger.info("Migrated "+i.get()+" artefacts of type 'CallFunction'");
	}
	
	@Override
	public void runDowngradeScript() {
		// TODO Auto-generated method stub
		
	}

}
