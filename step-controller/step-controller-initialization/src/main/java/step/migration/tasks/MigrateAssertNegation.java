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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.jongo.Mapper;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoDatabase;

import ch.exense.commons.app.ArgumentParser;
import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.Version;
import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.accessors.MongoClientSession;
import step.core.collections.mongodb.MongoDBCollection;
import step.core.plans.Plan;
import step.core.plans.PlanAccessorImpl;
import step.migration.MigrationTask;

/**
 * This function ensures that all the artefacts have their name saved properly in the attribute map. 
 * This will only be needed for the migration from 3.3.x or lower to 3.4.x or higher
 *
 */
public class MigrateAssertNegation extends MigrationTask {
	private MongoDatabase mongoDatabase;
	private com.mongodb.client.MongoCollection<Document> planCollection;
	private PlanAccessorImpl planAccessor;
	private Mapper dbLayerObjectMapper;
	private Unmarshaller unmarshaller;
	
	public MigrateAssertNegation() {
		super(new Version(3,13,3));
	}

	@Override
	protected void setContext(GlobalContext context) {
		super.setContext(context);
		init(mongoClientSession);
		context.put(MigrateAssertNegation.class, this);
	}
	
	protected void init(MongoClientSession mongoClientSession) {
		mongoDatabase = mongoClientSession.getMongoDatabase();
		planCollection = mongoDatabase.getCollection("plans");
		
		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		unmarshaller = dbLayerObjectMapper.getUnmarshaller();
	
		planAccessor = new PlanAccessorImpl(new MongoDBCollection<Plan>(mongoClientSession, "plans", Plan.class));
	}
	
	@Override
	public void runUpgradeScript() {
		modifyAssertNegationType(context);
	}
	
	private void retrieveAssertNodeRecursively(BasicDBList children, BasicDBList assertNodesToBeUpdated) {
		for(Object child : children) {
			if(((BasicDBObject) child).get("_class").equals("Assert")) {				
				assertNodesToBeUpdated.add(child);
			} else {
				retrieveAssertNodeRecursively((BasicDBList) ((BasicDBObject) child).get("children"), assertNodesToBeUpdated);
			}
		}
	}
	
	private void modifyAssertNegationType(GlobalContext context) {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Searching for artefacts of type 'Assert' to be migrated...");
		
		planCollection.find(BasicDBObject.class).iterator().forEachRemaining(p -> {
			try {
				BasicDBList children = new BasicDBList();
				BasicDBList assertNodesToBeUpdated = new BasicDBList();
				
				children = (BasicDBList) ((BasicDBObject) p.get("root")).get("children");
				retrieveAssertNodeRecursively(children, assertNodesToBeUpdated);
				
				assertNodesToBeUpdated.iterator().forEachRemaining(a -> {
					BasicDBObject assertNode = (BasicDBObject) a;
					try {
						if(assertNode.containsField("negate")) {
							logger.info("Migrating assert node " + assertNode.getString("_id") + ", found in plan " + p.getString("_id"));
							boolean currentNegateValue = assertNode.getBoolean("negate");					
							assertNode.remove("negate");
							
							Map<String, Object> doNegateMap = new HashMap<String,Object>();
							doNegateMap.put("dynamic", false);
							doNegateMap.put("value", currentNegateValue);
							assertNode.put("doNegate", new Document(doNegateMap));
							
							Plan unmarshalledPlan = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(p), Plan.class);
							planAccessor.save(unmarshalledPlan);
							
							successCount.incrementAndGet();
						}						
					} catch (Exception e) {
						errorCount.incrementAndGet();
						logger.error("Error while migrating assert " + assertNode, e);
					}				
				});
			} catch(Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating"
						+ " asserts from plan " + p, e);
			}
		});	
		logger.info("Migrated "+successCount.get()+" assert controls successfully.");
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating assert controls. See previous error logs for details.");
		}
	}

	public static void main(String[] args) throws IOException {
		ArgumentParser arguments = new ArgumentParser(args);
		Configuration configuration;
		if(arguments.hasOption("config")) {
			configuration = new Configuration(new File(arguments.getOption("config")));
		} else {
			configuration = new Configuration();
			configuration.putProperty("db.host", "localhost");
		}
		MongoClientSession mongoClientSession = new MongoClientSession(configuration);
		MigrateAssertNegation task = new MigrateAssertNegation();
		task.init(mongoClientSession);
		task.runUpgradeScript();
	}

	@Override
	public void runDowngradeScript() {
		// TODO Auto-generated method stub		
	}
}
