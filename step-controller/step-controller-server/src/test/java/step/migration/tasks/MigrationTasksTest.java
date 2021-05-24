package step.migration.tasks;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.core.collections.CollectionFactory;
import ch.exense.commons.core.collections.mongodb.MongoDBCollectionFactory;

public class MigrationTasksTest {

	public static void main(String[] args) {
		Configuration configuration = new Configuration();
		configuration.putProperty("db.host", "localhost");
		configuration.putProperty("db.database", "step");
		CollectionFactory collectionFactory = new MongoDBCollectionFactory(configuration);
		
		//collectionFactory = new InMemoryCollectionFactory(new Configuration());

//		new ScreenTemplateMigrationTask(collectionFactory).runUpgradeScript();
//		new SetSchedulerTaskAttributes(collectionFactory).runUpgradeScript();
//		MigrateArtefactsToPlans migrateArtefactsToPlans = new MigrateArtefactsToPlans(collectionFactory);
//		migrateArtefactsToPlans.runUpgradeScript();
//		
//		//new MigrateArtefactsToPlansEE(collectionFactory, migrateArtefactsToPlans).
//		new RemoveLocalFunctions(collectionFactory).runUpgradeScript();
//		new ScreenTemplateArtefactTableMigrationTask(collectionFactory).runUpgradeScript();
//		new MigrateAssertNegation(collectionFactory).runUpgradeScript();
		
		new MigrateSeleniumFunctions(collectionFactory, null).runUpgradeScript();;
	}

}
