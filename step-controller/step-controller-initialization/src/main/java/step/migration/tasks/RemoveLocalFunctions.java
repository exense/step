package step.migration.tasks;

import org.bson.Document;

import com.mongodb.client.result.DeleteResult;

import step.core.GlobalContext;
import step.core.Version;
import step.migration.MigrationTask;

/**
 * This task removes the functions of type 'LocalFunction'
 *
 */
public class RemoveLocalFunctions extends MigrationTask {

	public RemoveLocalFunctions() {
		super(new Version(3,13,0));
	}

	@Override
	public void runUpgradeScript() {
		removeLocalFunctions(context);
	}
	
	private void removeLocalFunctions(GlobalContext context) {
		logger.info("Searching for keywords of type 'LocalFunction' to be deleted...");
		
		com.mongodb.client.MongoCollection<Document> functions = context.getMongoClientSession().getMongoDatabase().getCollection("functions");
		
		Document filter = new Document("type", "step.functions.base.types.LocalFunction");
		DeleteResult result = functions.deleteMany(filter);
		
		logger.info("Removed "+result.getDeletedCount()+" keywords of type 'LocalFunction'");
	}
		
	@Override
	public void runDowngradeScript() {
		
	}

}
