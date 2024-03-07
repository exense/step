package step.migration.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.postgresql.PostgreSQLCollection;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

public class FixPostgreSQLIndexes extends MigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(FixPostgreSQLIndexes.class);
    public static final String TIME_SERIES_COLLECTION_PROPERTY = "timeseries";
    public static final String MEASUREMENTS_COLLECTION_PROPERTY = "measurements";
    public static final String FUNCTIONS_COLLECTION_PROPERTY = "functions";
    public static final String PLANS_COLLECTION_PROPERTY = "plans";
    public static final String TASKS_COLLECTION_PROPERTY = "tasks";

    public FixPostgreSQLIndexes(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3,24,3), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        dropPsqlIndex(TIME_SERIES_COLLECTION_PROPERTY, "idx_timeseries_attributes_eidasc");
        dropPsqlIndex(MEASUREMENTS_COLLECTION_PROPERTY, "idx_measurements_eidasc_beginasc");
        dropPsqlIndex(MEASUREMENTS_COLLECTION_PROPERTY, "idx_measurements_eidasc_typeasc_beginasc");
        dropPsqlIndex(MEASUREMENTS_COLLECTION_PROPERTY, "idx_measurements_planidasc_beginasc");
        dropPsqlIndex(MEASUREMENTS_COLLECTION_PROPERTY, "idx_measurements_taskidasc_beginasc");
        dropPsqlIndex(FUNCTIONS_COLLECTION_PROPERTY, "idx_functions_customfields_automationpackageidasc");
        dropPsqlIndex(TASKS_COLLECTION_PROPERTY, "idx_tasks_customfields_automationpackageidasc");
        dropPsqlIndex(PLANS_COLLECTION_PROPERTY, "idx_plans_customfields_automationpackageidasc");


    }

    private void dropPsqlIndex(String collectionName, String indexName) {
        Collection<Document> collection = collectionFactory.getCollection(collectionName, Document.class);
        if (collection instanceof PostgreSQLCollection) {
            logger.info("Removing postgresql index for collection '" + collectionName + "' and index '" + indexName + "'");
            collection.dropIndex(indexName);
            logger.info("Index removed.");
        }
    }

    @Override
    public void runDowngradeScript() {

    }
}
