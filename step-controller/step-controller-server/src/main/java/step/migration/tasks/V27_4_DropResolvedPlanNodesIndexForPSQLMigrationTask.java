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
            // Warning: due to the current implementation for few accessors directly created in the Controller.initContext
            // outside the plugin lifecycle, we for now need a direct reference to the accessor to properly recreate its context
            // after the migration. In this case we need to recreate the accessor which will recreate the related DB indexes
            new ResolvedPlanNodeAccessor(collectionFactory);
        }
    }


    @Override
    public void runDowngradeScript() {

    }
}
