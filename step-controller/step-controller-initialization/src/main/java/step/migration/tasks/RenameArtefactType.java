package step.migration.tasks;

import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;

import com.mongodb.Block;

import step.core.GlobalContext;
import step.core.Version;
import step.migration.MigrationTask;

/**
 * This function ensures that all the artefacts have their name saved properly in the attribute map. 
 * This will only be needed for the migration from 3.3.x or lower to 3.4.x or higher
 *
 */
public class RenameArtefactType extends MigrationTask {

	public RenameArtefactType() {
		super(new Version(3,5,0));
	}

	@Override
	public void runUpgradeScript() {
		renameArtefactType(context, "FunctionGroup", "Session");
		renameArtefactType(context, "CallFunction", "CallKeyword");
	}
	
	private void renameArtefactType(GlobalContext context, String classFrom, String classTo) {
		logger.info("Searching for artefacts of type '"+classFrom+"' to be migrated...");
		com.mongodb.client.MongoCollection<Document> artefacts = context.getMongoClientSession().getMongoDatabase().getCollection("artefacts");
		
		AtomicInteger i = new AtomicInteger();
		Document filterCallFunction = new Document("_class", classFrom);
		artefacts.find(filterCallFunction).forEach(new Block<Document>() {

			@Override
			public void apply(Document t) {
				try {
					i.incrementAndGet();
					t.put("_class", classTo);
					
					Document filter = new Document("_id", t.get("_id"));
					
					artefacts.replaceOne(filter, t);
					logger.info("Migrating "+classFrom+ " to "+t.toJson());
				} catch(ClassCastException e) {
					// ignore
				}
			}
		});
		
		logger.info("Migrated "+i.get()+" artefacts of type '"+classFrom+"'");
	}
	
	@Override
	public void runDowngradeScript() {
		// TODO Auto-generated method stub
		
	}

}
