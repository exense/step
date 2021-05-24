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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.types.ObjectId;

import step.core.Version;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.CollectionFactory;
import ch.exense.commons.core.collections.Document;
import ch.exense.commons.core.collections.DocumentObject;
import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.Filters;
import step.core.imports.converter.ArtefactsToPlans;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

/**
 * This task migrates the collection 'artefacts' to the collection 'plans' which has been introduced in 3.13 
 *
 */
public class MigrateArtefactsToPlans extends MigrationTask {

	private final Collection<Document> artefactCollection;
	private final Collection<Document> functionCollection;
	private final Collection<Document> executionCollection;
	private final Collection<Document> tasksCollection;
	private final Map<ObjectId, ObjectId> artefactIdToPlanId;
	private final ArtefactsToPlans artefactsToPlans;
	private final Collection<Document> planCollection;

	public MigrateArtefactsToPlans(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,13,0), collectionFactory, migrationContext);
		
		artefactCollection = collectionFactory.getCollection("artefacts", Document.class);
		planCollection = collectionFactory.getCollection("plans", Document.class);
		executionCollection = collectionFactory.getCollection("executions", Document.class);
		functionCollection = collectionFactory.getCollection("functions", Document.class);
		tasksCollection = collectionFactory.getCollection("tasks", Document.class);

		artefactsToPlans = new ArtefactsToPlans(artefactCollection, planCollection);
		artefactIdToPlanId = artefactsToPlans.getArtefactIdToPlanId();
		
		migrationContext.put(MigrateArtefactsToPlans.class, this);
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
		artefactCollection.rename(newArtefactsCollectionName);
	}
	
	private void migrateCompositeFunctionsFunctions() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		
		Filter filterCompositeFunction = Filters.equals("type", "step.plugins.functions.types.CompositeFunction");
		functionCollection.find(filterCompositeFunction, null, null, null, 0).forEach(t -> {
			try {
				if(t.containsKey("artefactId")) {
					String id = t.getId().toString();
					String artefactId = t.getString("artefactId");
					
					Document rootArtefact = artefactCollection.find(Filters.id(artefactId), null, null, null, 0).findFirst().get();
					if(rootArtefact != null) {
						Document plan = artefactsToPlans.migrateArtefactToPlan(rootArtefact, false);
						if(plan != null) {
							ObjectId planId = plan.getId();
							t.put("planId", planId.toString());
							t.remove("artefactId");
							
							functionCollection.save(t);
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
		executionCollection.find(Filters.empty(), null, null, null, 0).forEach(t -> {
			try {
				DocumentObject object = t.getObject("executionParameters");
				ExecutionParametersMigrationResult executionParameterMigrationResult = migrateExecutionParameter(object);
				if(executionParameterMigrationResult.executionParametersUpdated) {
					t.put("planId", executionParameterMigrationResult.planId);
					executionCollection.save(t);
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
	
	protected ExecutionParametersMigrationResult migrateExecutionParameter(DocumentObject object) {
		ExecutionParametersMigrationResult result = new ExecutionParametersMigrationResult();
		if(object != null) {
			DocumentObject artefact = object.getObject("artefact");
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

	protected String migrateRepositoryObjectReference(DocumentObject artefact) {
		String result = null;
		ObjectId planId;
		DocumentObject repositoryParameters = artefact.getObject("repositoryParameters");
		if(repositoryParameters != null) {
			String artefactId = (String) repositoryParameters.get("artefactid");
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
		tasksCollection.find(Filters.empty(), null, null, null, 0).forEach(t -> {
			DocumentObject executionsParameters = t.getObject("executionsParameters");
			ExecutionParametersMigrationResult executionParameterMigrationResult = migrateExecutionParameter(executionsParameters);
			if(executionParameterMigrationResult.executionParametersUpdated) {
				tasksCollection.save(t);
			}
		});
	}
	
	@Override
	public void runDowngradeScript() {
	}
}
