package step.migration.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNodeAccessor;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.postgresql.PostgreSQLCollection;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

public class V27_4_DropResolvedPlanNodesIndexForPSQLMigrationTask extends MigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(V27_4_DropResolvedPlanNodesIndexForPSQLMigrationTask.class);
    public static final String RESOLVED_PLAN_COLLECTION_PROPERTY = "resolvedPlans";

    public V27_4_DropResolvedPlanNodesIndexForPSQLMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3,27,4), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        String indexName = "idx_resolvedplans_parentidasc";
        Collection<Document> collection = collectionFactory.getCollection(RESOLVED_PLAN_COLLECTION_PROPERTY, Document.class);
        if (collection instanceof PostgreSQLCollection) {
            logger.info("Removing postgresql index for collection '" + RESOLVED_PLAN_COLLECTION_PROPERTY + "' and index '" + indexName + "'");
            collection.dropIndex(indexName);
            logger.info("Index removed.");
            //since this accessor and related indexes are created directly in the controller class and not in a plugin,
            // the accessor and indexes are created before the migration, so we must recreate them after dropping the old one
            new ResolvedPlanNodeAccessor(collectionFactory);
        }
    }


    @Override
    public void runDowngradeScript() {

    }
}
