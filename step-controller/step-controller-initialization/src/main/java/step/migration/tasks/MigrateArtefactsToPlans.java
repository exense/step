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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jongo.Mapper;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoDatabase;

import ch.exense.commons.app.ArgumentParser;
import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.Version;
import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.accessors.FunctionAccessorImpl;
import step.core.accessors.MongoClientSession;
import step.core.accessors.PlanAccessorImpl;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessorImpl;
import step.core.imports.converter.ArtefactsToPlans;
import step.core.plans.Plan;
import step.core.scheduler.ExecutionTaskAccessorImpl;
import step.core.scheduler.ExecutiontTaskParameters;
import step.functions.accessor.FunctionAccessor;
import step.migration.MigrationTask;
import step.plugins.functions.types.CompositeFunction;

/**
 * This task migrates the collection 'artefacts' to the collection 'plans' which has been introduced in 3.13 
 *
 */
public class MigrateArtefactsToPlans extends MigrationTask {

	private MongoDatabase mongoDatabase;
	private com.mongodb.client.MongoCollection<Document> artefactCollection;
	private com.mongodb.client.MongoCollection<Document> functionCollection;
	private com.mongodb.client.MongoCollection<Document> executionCollection;
	private com.mongodb.client.MongoCollection<Document> tasksCollection;
	private ExecutionAccessorImpl executionAccessor;
	private ExecutionTaskAccessorImpl executionTaskAccessor;
	private FunctionAccessor functionAccessor;
	private Mapper dbLayerObjectMapper;
	private Map<ObjectId, ObjectId> artefactIdToPlanId;
	private Unmarshaller unmarshaller;
	private ArtefactsToPlans artefactsToPlans;

	public MigrateArtefactsToPlans() {
		super(new Version(3,13,0));
	}

	@Override
	protected void setContext(GlobalContext context) {
		super.setContext(context);
		init(context.getMongoClientSession());
		context.put(MigrateArtefactsToPlans.class, this);
	}

	protected void init(MongoClientSession mongoClientSession) {
		mongoDatabase = mongoClientSession.getMongoDatabase();
		artefactCollection = mongoDatabase.getCollection("artefacts");
		executionCollection = mongoDatabase.getCollection("executions");
		functionCollection = mongoDatabase.getCollection("functions");
		tasksCollection = mongoDatabase.getCollection("tasks");
		
		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		unmarshaller = dbLayerObjectMapper.getUnmarshaller();
		
		executionAccessor = new ExecutionAccessorImpl(mongoClientSession);
		executionTaskAccessor = new ExecutionTaskAccessorImpl(mongoClientSession);
		functionAccessor = new FunctionAccessorImpl(mongoClientSession);

		artefactsToPlans = new ArtefactsToPlans(artefactCollection,
				new PlanAccessorImpl(mongoClientSession));
		artefactIdToPlanId = artefactsToPlans.getArtefactIdToPlanId();
	}

	@Override
	public void runUpgradeScript() {
		int count = artefactsToPlans.getNbPlans();
		logger.info("Found "+count+" root artefacts to be migrated. Starting migration...");
		
		artefactsToPlans.migrateArtefactsToPlans();
		migrateCompositeFunctionsFunctions();
		migrateExecutions();
		migrateSchedulerTasks();
		renameArtefactCollection();
	}

	protected void renameArtefactCollection() {
		String newArtefactsCollectionName = "artefacts_migrated";
		logger.info("Renaming collection 'artefacts' to '"+newArtefactsCollectionName+"'. This collection won't be used by step anymore. You can drop it if all your plans have been migrated without error.");
		artefactCollection.renameCollection(new MongoNamespace(mongoDatabase.getName(), newArtefactsCollectionName));
	}
	
	private void migrateCompositeFunctionsFunctions() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		
		Document filterCompositeFunction = new Document("type", CompositeFunction.class.getName());
		functionCollection.find(filterCompositeFunction, BasicDBObject.class).iterator().forEachRemaining(t -> {
			try {
				if(t.containsField("artefactId")) {
					String id = t.getString("_id");
					String artefactId = t.getString("artefactId");
					
					BasicDBObject rootArtefact = artefactCollection.find(new Document("_id", new ObjectId(artefactId)), BasicDBObject.class).first();
					if(rootArtefact != null) {
						Plan plan = artefactsToPlans.migrateArtefactToPlan(rootArtefact);
						if(plan != null) {
							ObjectId planId = plan.getId();
							t.put("planId", planId);
							t.remove("artefactId");
							CompositeFunction compositeFunction = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(t), CompositeFunction.class);
							functionAccessor.save(compositeFunction);
							successCount.incrementAndGet();
						} else {
							errorCount.incrementAndGet();
							logger.error("Error while migrating plan for composite function " + id + " with artefactId "+artefactId);
						}
					} else {
						errorCount.incrementAndGet();
						logger.error("Unable to find root artefact for composite function " + id + " with artefactId "+artefactId);
					}
				}
			} catch (Exception e) {
				errorCount.incrementAndGet();
				logger.error("Unexpected error while migrating composite function " + t, e);
			}
		});
		
		logger.info("Migrated "+successCount.get()+" composite functions successfully.");
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating composite functions. See previous error logs for details.");
		}
	}

	private void migrateExecutions() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		
		logger.info("Searching for executions be migrated...");
		executionCollection.find(BasicDBObject.class).iterator().forEachRemaining(t -> {
			try {
				BasicDBObject object = (BasicDBObject) t.get("executionParameters");
				ExecutionParametersMigrationResult executionParameterMigrationResult = migrateExecutionParameter(object);
				if(executionParameterMigrationResult.executionParametersUpdated) {
					// ... and save the result while ensuring integrity by unmarshalling as POJO
					Execution execution = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(t), Execution.class);
					execution.setPlanId(executionParameterMigrationResult.planId);
					executionAccessor.save(execution);
					successCount.incrementAndGet();
				}
			} catch (Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating execution " + t, e);
			}
		});
		logger.info("Migrated "+successCount.get()+" executions successfully.");
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating executions. See previous error logs for details.");
		}
	}
	
	protected static class ExecutionParametersMigrationResult {
		boolean executionParametersUpdated;
		String planId;
	}
	
	protected ExecutionParametersMigrationResult migrateExecutionParameter(BasicDBObject object) {
		ExecutionParametersMigrationResult result = new ExecutionParametersMigrationResult();
		if(object != null) {
			BasicDBObject artefact = (BasicDBObject) object.get("artefact");
			if(artefact != null) {
				// Rename the field "repositoryParameters.artefactid" to "repositoryParameters.planid"
				String planIdString = migrateRepositoryObjectReference(artefact);
				result.planId = planIdString;
				
				// Rename the field "artefact" to "repositoryObject"
				object.put("repositoryObject", artefact);
				object.remove("artefact");
				result.executionParametersUpdated = true;
			}
		}
		return result;
	}

	protected String migrateRepositoryObjectReference(BasicDBObject artefact) {
		String result = null;
		ObjectId planId;
		BasicDBObject repositoryParameters = (BasicDBObject) artefact.get("repositoryParameters");
		if(repositoryParameters != null) {
			String artefactId = repositoryParameters.getString("artefactid");
			if(artefactId != null) {
				planId = artefactIdToPlanId.get(new ObjectId(artefactId));
				if(planId != null) {
					String planIdString = planId.toString();
					repositoryParameters.put("planid", planIdString);
					result = planIdString;
				}
				repositoryParameters.remove("artefactid");
			}
		}
		return result;
	}
	
	private void migrateSchedulerTasks() {
		tasksCollection.find(BasicDBObject.class).iterator().forEachRemaining(t -> {
			BasicDBObject executionsParameters = (BasicDBObject) t.get("executionsParameters");
			ExecutionParametersMigrationResult executionParameterMigrationResult = migrateExecutionParameter(executionsParameters);
			if(executionParameterMigrationResult.executionParametersUpdated) {
				ExecutiontTaskParameters executionTaskParameters = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(t), ExecutiontTaskParameters.class);
				executionTaskAccessor.save(executionTaskParameters);
			}
		});
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
		MigrateArtefactsToPlans task = new MigrateArtefactsToPlans();
		task.init(mongoClientSession);
		task.runUpgradeScript();
	}
	
	@Override
	public void runDowngradeScript() {
	}
}
