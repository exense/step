package step.migration.tasks;

import step.core.Version;
import step.core.collections.*;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class MigrateParametersToDynamicValues extends MigrationTask {


    private final Collection<Document> parameters;
    private final Collection<Document> parametersversions;

    public MigrateParametersToDynamicValues(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3,26,0), collectionFactory, migrationContext);
        parameters = collectionFactory.getCollection("parameters", Document.class);
        parametersversions = collectionFactory.getCollection("parametersversions", Document.class);
    }

    @Override
    public void runUpgradeScript() {
        logger.info("Migrating all parameters to support dynamic values...");

        AtomicInteger count = new AtomicInteger();
        try (Stream<Document> documentStream = parameters.findLazy(Filters.empty(), null, null, null, 0)) {
            documentStream.forEach(d -> {
                String value = d.getString("value");
                //value might be null for protected parameters and will stay null in that case
                if (value != null) {
                    Document dynamicValue = new Document();
                    dynamicValue.put("dynamic", false);
                    dynamicValue.put("value", value);
                    d.put("value", dynamicValue);
                    parameters.save(d);
                }
            });
        }
        logger.info("Migrated {} parameters to support dynamic values", count.get());

        logger.info("Migrating all parameters versions to support dynamic values...");

        count = new AtomicInteger();
        try (Stream<Document> documentStream = parametersversions.findLazy(Filters.empty(), null, null, null, 0)) {
            documentStream.forEach(v -> {
                DocumentObject d = v.getObject("entity");
                String value = d.getString("value");
                //value might be null for protected parameters and will stay null in that case
                if (value != null) {
                    Document dynamicValue = new Document();
                    dynamicValue.put("dynamic", false);
                    dynamicValue.put("value", value);
                    d.put("value", dynamicValue);
                    parametersversions.save(v);
                }
            });
        }
        logger.info("Migrated {} parameters versions to support dynamic values", count.get());
    }

    @Override
    public void runDowngradeScript() {

    }
}
