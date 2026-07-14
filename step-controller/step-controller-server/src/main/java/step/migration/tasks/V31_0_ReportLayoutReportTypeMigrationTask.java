package step.migration.tasks;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.entities.EntityConstants;
import step.core.reporting.model.ReportLayout;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Backfills the {@link ReportLayout#FIELD_REPORT_TYPE} field on report layout documents created before the
 * cross-execution reporting feature was introduced. Without this field, filtered queries for
 * {@link ReportLayout.ReportLayoutType#SingleExecution} would not match legacy documents on Mongo/PSQL backends,
 * making previously released layouts disappear from the single-execution view.
 */
public class V31_0_ReportLayoutReportTypeMigrationTask extends MigrationTask {

    public V31_0_ReportLayoutReportTypeMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3, 31, 0), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        Collection<Document> reportLayouts = collectionFactory.getCollection(EntityConstants.reportLayouts, Document.class);
        AtomicLong counter = new AtomicLong(0);
        logger.info("Backfilling '{}' on report layouts missing it...", ReportLayout.FIELD_REPORT_TYPE);
        try (Stream<Document> documents = reportLayouts.find(Filters.not(Filters.exists(ReportLayout.FIELD_REPORT_TYPE)), null, null, null, 0)) {
            documents.forEach(document -> {
                document.put(ReportLayout.FIELD_REPORT_TYPE, ReportLayout.ReportLayoutType.SingleExecution.name());
                reportLayouts.save(document);
                counter.incrementAndGet();
            });
        }
        logger.info("Completed: {} report layouts migrated to reportType '{}'.", counter.get(), ReportLayout.ReportLayoutType.SingleExecution.name());
    }

    @Override
    public void runDowngradeScript() {
    }
}
