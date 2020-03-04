package step.migration.tasks;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jongo.Mapper;
import org.jongo.marshall.jackson.JacksonMapper;

import com.mongodb.BasicDBObject;

import ch.exense.commons.app.ArgumentParser;
import ch.exense.commons.app.Configuration;
import step.artefacts.CallPlan;
import step.core.GlobalContext;
import step.core.Version;
import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.accessors.MongoClientSession;
import step.core.accessors.PlanAccessorImpl;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.migration.MigrationTask;

/**
 * This task migrates the collection 'artefacts' to the collection 'plans' which has been introduced in 3.13 
 *
 */
public class MigrateArtefactsToPlans extends MigrationTask {

	private static final String CHILDREN_ID_FIELD = "childrenIDs";
	private com.mongodb.client.MongoCollection<Document> artefactCollection;
	private PlanAccessorImpl planAccessor;
	private Mapper dbLayerObjectMapper;
	private Map<ObjectId, ObjectId> artefactIdToPlanId;

	public MigrateArtefactsToPlans() {
		super(new Version(3,13,0));
	}

	@Override
	protected void setContext(GlobalContext context) {
		super.setContext(context);
		init(context.getMongoClientSession());
	}

	protected void init(MongoClientSession mongoClientSession) {
		artefactCollection = mongoClientSession.getMongoDatabase().getCollection("artefacts");
		
		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		
		planAccessor = new PlanAccessorImpl(mongoClientSession);
	}

	@Override
	public void runUpgradeScript() {
		migrateArtefactsToPlans();
	}
	
	private void migrateArtefactsToPlans() {
		
		artefactIdToPlanId = new HashMap<>();
		
		AtomicInteger i = new AtomicInteger();
		Document filterRootArtefacts = new Document("root", true);
		
		logger.info("Searching for root artefacts to be migrated...");
		artefactCollection.find(filterRootArtefacts, BasicDBObject.class).iterator().forEachRemaining(t -> {
			ObjectId objectId = t.getObjectId("_id");
			artefactIdToPlanId.put(objectId, new ObjectId());
			i.incrementAndGet();
		});
		
		logger.info("Found "+i.get()+" root artefacts to be migrated. Starting migration...");
		AtomicInteger errorCount = new AtomicInteger();
		i.set(0);
		artefactCollection.find(filterRootArtefacts, BasicDBObject.class).iterator().forEachRemaining(t -> {
			Map<String, String> attributes = new HashMap<>();
			try {
				BasicDBObject document = (BasicDBObject)t.get("attributes");
				document.keySet().forEach(key->{
					attributes.put(key, document.getString(key));
				});
				
				AbstractArtefact artefact = unmarshallArtefact(t);
				
				Plan plan = new Plan();
				
				plan.setId(artefactIdToPlanId.get(artefact.getId()));
				plan.setAttributes(attributes);
				plan.setRoot(artefact);
				plan.setVisible(true);
				
				logger.info("Migrated plan "+attributes);
				
				planAccessor.save(plan);
				i.incrementAndGet();
			} catch(Exception e) {
				logger.error("Error while migrating plan "+attributes, e);
				errorCount.incrementAndGet();
			}
		});
		
		logger.info("Migrated "+i.get()+" artefacts successfully.");
		if(errorCount.get()>0) {
			logger.error(errorCount.get() + " artefacts couldn't be migrated. See error logs for details");
		}
	}

	@SuppressWarnings("unchecked")
	protected AbstractArtefact unmarshallArtefact(BasicDBObject t) {
		List<ObjectId> childrendIDs = null;
		if(t.containsField(CHILDREN_ID_FIELD)) {
			childrendIDs = (List<ObjectId>) t.get(CHILDREN_ID_FIELD);
		}
		t.remove(CHILDREN_ID_FIELD);
		
		AbstractArtefact artefact = dbLayerObjectMapper.getUnmarshaller().unmarshall(org.jongo.bson.Bson.createDocument(t), AbstractArtefact.class);
		
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
				BasicDBObject child = artefactCollection.find(new Document("_id", childID), BasicDBObject.class).first();
				AbstractArtefact artefactChild = unmarshallArtefact(child);
				artefact.getChildren().add(artefactChild);
			});
		}
		
		return artefact;
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
		task.migrateArtefactsToPlans();
	}
	
	@Override
	public void runDowngradeScript() {
	}
}
