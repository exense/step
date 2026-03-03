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

import java.util.List;

public class V29_2_TimeSeriesNewIndexes extends MigrationTask {


    private static final Logger log = LoggerFactory.getLogger(V29_2_TimeSeriesNewIndexes.class);

    public V29_2_TimeSeriesNewIndexes(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3,29,2), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        // Compound index created for timeseries collection with metricType, taskId and begin where wrongly using a string type for begin, we need to remove them.
        // The correct ones are then created automatically in the initializeData hook as for a fresh setup
        // This type of the field is only relevant for the PSQL indexes
        List<String> suffix = List.of("", "_minute", "_hour", "_day", "_week");
        suffix.forEach(s -> {
            String collectionName = "timeseries" + s;
            String indexName = "idx_" + collectionName + "_attributes_taskidasc_attributes_metrictypeasc_beginasc";
            Collection<Document> collection = collectionFactory.getCollection(collectionName, Document.class);
            if (collection instanceof PostgreSQLCollection) {
                collection.dropIndex(indexName);
                log.info("Time-series index migration - dropped index {}", indexName);
            }
        });
    }

    @Override
    public void runDowngradeScript() {

    }
}
