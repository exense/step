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
package step.core.imports.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jongo.Mapper;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;

import step.artefacts.CallPlan;
import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;


/**
 * This task migrates the collection 'artefacts' to the collection 'plans' which has been introduced in 3.13 
 *
 */
public class ArtefactsToPlans  {
	
	private static final Logger logger = LoggerFactory.getLogger(ArtefactsToPlans.class);

	private static final String CHILDREN_ID_FIELD = "childrenIDs";
	private MongoCollection<Document> artefactCollection;
	private PlanAccessor planAccessor;
	
	private Mapper dbLayerObjectMapper;
	private Map<ObjectId, ObjectId> artefactIdToPlanId;
	private Unmarshaller unmarshaller;
	private int nbPlans = 0;
	private ObjectEnricher objectEnricher = null;

	public ArtefactsToPlans(MongoCollection<Document> artefacts, PlanAccessor accessor) {
		this(artefacts,accessor,null);
	}
	
	public ArtefactsToPlans(MongoCollection<Document> artefacts, PlanAccessor accessor, ObjectEnricher objectEnricher) {
		planAccessor = accessor;
		artefactCollection = artefacts;
		
		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		unmarshaller = dbLayerObjectMapper.getUnmarshaller();
		
		this.objectEnricher = objectEnricher;
		artefactIdToPlanId = new HashMap<>();
		generatePlanIds();
	}
	
	private void generatePlanIds() {
		logger.info("Searching for root artefacts to be migrated...");
		AtomicInteger count = new AtomicInteger();
		Document filterRootArtefacts = new Document("root", true);
		artefactCollection.find(filterRootArtefacts, BasicDBObject.class).iterator().forEachRemaining(t -> {
			try {
				ObjectId objectId = t.getObjectId("_id");
				artefactIdToPlanId.put(objectId, new ObjectId());
				count.incrementAndGet();
			} catch (Exception e) {
				logger.error("Invalid object id found for the root artefact",e);
			}
		});
		nbPlans = count.get();
	}
	
	public int migrateArtefactsToPlans() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();

		Document filterRootArtefacts = new Document("root", true);
		artefactCollection.find(filterRootArtefacts, BasicDBObject.class).iterator().forEachRemaining(t -> {
			migrateArtefactToPlan(successCount, errorCount, t);
		});
		
		logger.info("Migrated "+successCount.get()+" artefacts successfully.");
		if(errorCount.get()>0) {
			logger.error(errorCount.get() + " artefacts couldn't be migrated. See error logs for details");
		}
		
		successCount.set(0);
		int errors = errorCount.get(); 
		errorCount.set(0);
		
		return errors;
	}

	public Plan migrateArtefactToPlan(BasicDBObject t) {
		return migrateArtefactToPlan(null, null, t);
	}
	
	protected Plan migrateArtefactToPlan(AtomicInteger successCount, AtomicInteger errorCount, BasicDBObject t) {
		Map<String, String> attributes = new HashMap<>();
		try {
			BasicDBObject document = (BasicDBObject)t.get("attributes");
			if(document != null) {
				document.keySet().forEach(key->{
					attributes.put(key, document.getString(key));
				});
			}
			
			AbstractArtefact artefact = unmarshallArtefact(t);
			
			Plan plan = new Plan();
			
			plan.setId(artefactIdToPlanId.get(artefact.getId()));
			plan.setAttributes(attributes);
			plan.setRoot(artefact);
			plan.setVisible(true);
			
			logger.info("Migrated plan "+attributes);
			if (objectEnricher != null) {
				objectEnricher.accept(plan);
			}
			plan = planAccessor.save(plan);
			if(successCount != null) {
				successCount.incrementAndGet();
			}
			return plan;
		} catch(Exception e) {
			logger.error("Error while migrating plan "+attributes, e);
			if(errorCount != null) {
				errorCount.incrementAndGet();
			}
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private AbstractArtefact unmarshallArtefact(BasicDBObject t) {
		List<Object> childrendIDs = null;
		if(t.containsField(CHILDREN_ID_FIELD)) {
			childrendIDs = (List<Object>) t.get(CHILDREN_ID_FIELD);
		}
		t.remove(CHILDREN_ID_FIELD);
		
		AbstractArtefact artefact = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(t), AbstractArtefact.class);
		
		if(artefact instanceof CallPlan) {
			String artefactId = t.getString("artefactId");
			if(artefactId!=null) {
				ObjectId referencedPlanId = artefactIdToPlanId.get(new ObjectId(artefactId));
				if(referencedPlanId != null) {
					((CallPlan) artefact).setPlanId(referencedPlanId.toString());
				} else {
					logger.warn("The artefact "+artefactId+" referenced by the artefact (call plan) "+t.getObjectId("_id").toString()+" doesn't exist");
				}
			} else {
				// Call by attributes => nothing to do as we're assigning the attributes of the root artefact to the plan
			}
		}
		
		if(childrendIDs!=null) {
			childrendIDs.forEach(childID->{
				//required to support both migration from database and exported JSON
				ObjectId  childObjectId = (childID instanceof ObjectId) ? (ObjectId) childID : new ObjectId((String) childID);
				BasicDBObject child = artefactCollection.find(new Document("_id", childObjectId), BasicDBObject.class).first();
				AbstractArtefact artefactChild = unmarshallArtefact(child);
				artefact.getChildren().add(artefactChild);
			});
		}
		
		return artefact;
	}	
	
	public int getNbPlans() {
		return nbPlans;
	}
}
