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
        collectionFactory.getCollection("timeseries_hour", Document.class).drop();
        collectionFactory.getCollection("timeseries_day", Document.class).drop();
        collectionFactory.getCollection("timeseries_week", Document.class).drop();

        collectionFactory.getCollection("reportNodeTimeSeries_hour", Document.class).drop();
        collectionFactory.getCollection("reportNodeTimeSeries_day", Document.class).drop();
        collectionFactory.getCollection("reportNodeTimeSeries_week", Document.class).drop();
    }

    @Override
    public void runDowngradeScript() {

    }
}
