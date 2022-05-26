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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.core.objectenricher.ObjectEnricher;


/**
 * This task migrates the collection 'artefacts' to the collection 'plans' which has been introduced in 3.13 
 *
 */
public class ArtefactsToPlans  {
	
	private static final Logger logger = LoggerFactory.getLogger(ArtefactsToPlans.class);

	private static final String CHILDREN_ID_FIELD = "childrenIDs";
	private Collection<Document> artefactCollection;
	private Collection<Document> planCollection;
	
	private final Map<ObjectId, ObjectId> artefactIdToPlanId;
	private int nbPlans = 0;
	
	public ArtefactsToPlans(Collection<Document> artefacts, Collection<Document> plans) {
		artefactCollection = artefacts;
		planCollection = plans;

		artefactIdToPlanId = new HashMap<>();
		generatePlanIds();
	}
	
	private void generatePlanIds() {
		logger.info("Searching for root artefacts to be migrated...");
		AtomicInteger count = new AtomicInteger();
		
		rootArtefactStream().forEach(t -> {
			try {
				artefactIdToPlanId.put(t.getId(), new ObjectId());
				count.incrementAndGet();
			} catch (Exception e) {
				logger.error("Invalid object id found for the root artefact",e);
			}
		});
		nbPlans = count.get();
	}

	private Stream<Document> rootArtefactStream() {
		return artefactCollection.find(Filters.equals("root", true), null, null, null, 0);
	}
	
	public int migrateArtefactsToPlans() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();

		rootArtefactStream().forEach(t -> {
			migrateArtefactToPlan(successCount, errorCount, t, true);
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

	public Document migrateArtefactToPlan(Document t, boolean visiblePlan) {
		return migrateArtefactToPlan(null, null, t, visiblePlan);
	}
	
	protected Document migrateArtefactToPlan(AtomicInteger successCount, AtomicInteger errorCount, Document t, boolean visiblePlan) {
		Map<String, String> attributes = new HashMap<>();
		try {
			DocumentObject document = t.getObject("attributes");
			if(document != null) {
				document.keySet().forEach(key->{
					attributes.put(key, document.get(key).toString());
				});
			}
			
			Document plan = new Document();
			
			plan.put(AbstractIdentifiableObject.ID, artefactIdToPlanId.get(t.getId().toString()));
			plan.put("attributes", attributes);
			plan.put("root", migrateArtefact(t));
			plan.put("visible", visiblePlan);
			plan.put("_class", "step.core.plans.Plan");
			
			logger.info("Migrated plan "+attributes);
			
			plan = planCollection.save(plan);
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
	private Document migrateArtefact(Document t) {
		List<Object> childrendIDs = null;
		if(t.containsKey(CHILDREN_ID_FIELD)) {
			childrendIDs = (List<Object>) t.get(CHILDREN_ID_FIELD);
		}
		t.remove(CHILDREN_ID_FIELD);
		
		if(t.get("_class").equals("CallPlan")) {
			String artefactId = (String) t.get("artefactId");
			if(artefactId!=null) {
				ObjectId referencedPlanId = artefactIdToPlanId.get(new ObjectId(artefactId));
				if(referencedPlanId != null) {
					t.put("planId", referencedPlanId.toString());
				} else {
					logger.warn("The artefact "+artefactId+" referenced by the artefact (call plan) "+t.get(AbstractIdentifiableObject.ID).toString()+" doesn't exist");
				}
			} else {
				// Call by attributes => nothing to do as we're assigning the attributes of the root artefact to the plan
			}
		}
		
		if(childrendIDs!=null) {
			childrendIDs.forEach(childID->{
				//required to support both migration from database and exported JSON
				ObjectId  childObjectId = (childID instanceof ObjectId) ? (ObjectId) childID : new ObjectId((String) childID);
				Document child = artefactCollection.find(Filters.equals(AbstractIdentifiableObject.ID, childObjectId), null, null, null, 0).findFirst().get();
				Document artefactChild = migrateArtefact(child);
				((List<Document>) t.computeIfAbsent("children", k->new ArrayList<Document>())).add(artefactChild);
			});
		}
		
		return t;
	}	
	
	public int getNbPlans() {
		return nbPlans;
	}

	public Map<ObjectId, ObjectId> getArtefactIdToPlanId() {
		return artefactIdToPlanId;
	}
}
