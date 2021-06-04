package step.migration.tasks;

import java.util.Properties;

import step.core.collections.CollectionFactory;
import step.core.collections.mongodb.MongoDBCollectionFactory;

public class MigrationTasksTest {

	public static void main(String[] args) {
		Properties properties = new Properties();
		properties.put("host", "localhost");
		properties.put("database", "step310-1");
		CollectionFactory collectionFactory = new MongoDBCollectionFactory(properties);
		
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
