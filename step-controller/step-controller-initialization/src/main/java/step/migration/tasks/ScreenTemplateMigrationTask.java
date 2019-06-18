package step.migration.tasks;

import org.bson.Document;

import com.mongodb.Block;

import step.core.Version;
import step.migration.MigrationTask;

/**
 * This task migrates the screen inputs from screen 'functionTable'
 * which require the prefix "attributes." as of 3.11
 *
 */
public class ScreenTemplateMigrationTask extends MigrationTask {

	public ScreenTemplateMigrationTask() {
		super(new Version(3,11,0));
	}

	@Override
	public void runUpgradeScript() {
		com.mongodb.client.MongoCollection<Document> screenInputs = context.getMongoClientSession().getMongoDatabase().getCollection("screenInputs");
		Document filter = new Document("screenId", "functionTable");
		screenInputs.find(filter).forEach(new Block<Document>() {
			@Override
			public void apply(Document t) {
				try {
					Document input = (Document) t.get("input");
					if(input.getString("id").equals("name")) {
						input.put("id", "attributes.name");
					}
					
					Document filter = new Document("_id", t.get("_id"));
					screenInputs.replaceOne(filter, t);
					logger.info("Migrating screen input to "+t.toJson());
				} catch(ClassCastException e) {
					// ignore
				}
			}
		});
	}
	
	@Override
	public void runDowngradeScript() {
		
	}

}
