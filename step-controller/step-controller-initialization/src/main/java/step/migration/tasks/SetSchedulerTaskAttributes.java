package step.migration.tasks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.Block;

import step.artefacts.CallFunction;
import step.artefacts.CallPlan;
import step.core.Version;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutionTaskAccessorImpl;
import step.core.scheduler.ExecutiontTaskParameters;
import step.functions.Function;
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
		com.mongodb.client.MongoCollection<Document> tasks = context.getMongoClientSession().getMongoDatabase().getCollection("tasks");
		
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
