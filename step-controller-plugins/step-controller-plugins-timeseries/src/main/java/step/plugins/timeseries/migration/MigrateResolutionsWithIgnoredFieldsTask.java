package step.plugins.timeseries.migration;

import step.core.Version;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

public class MigrateResolutionsWithIgnoredFieldsTask extends MigrationTask {

    public MigrateResolutionsWithIgnoredFieldsTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3,26,2), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        //We have to use drop, otherwise the estimated count used to check if the collection is empty won't work with PSQL
        //This the collection are created in the startServer hook and this comes after, we still have to recreate the collection here after dropping
        //To make sure the table in postgres are recreated.
        collectionFactory.getCollection("timeseries_hour", Document.class).drop();
        collectionFactory.getCollection("timeseries_day", Document.class).drop();
        collectionFactory.getCollection("timeseries_week", Document.class).drop();
        //recreate tables, index are creted in the initializeData hook
        collectionFactory.getCollection("timeseries_hour", Document.class);
        collectionFactory.getCollection("timeseries_day", Document.class);
        collectionFactory.getCollection("timeseries_week", Document.class);

        collectionFactory.getCollection("reportNodeTimeSeries_hour", Document.class).drop();
        collectionFactory.getCollection("reportNodeTimeSeries_day", Document.class).drop();
        collectionFactory.getCollection("reportNodeTimeSeries_week", Document.class).drop();
        //recreate tables
        collectionFactory.getCollection("reportNodeTimeSeries_hour", Document.class);
        collectionFactory.getCollection("reportNodeTimeSeries_day", Document.class);
        collectionFactory.getCollection("reportNodeTimeSeries_week", Document.class);
    }

    @Override
    public void runDowngradeScript() {

    }
}
