package step.migration.tasks;

import org.junit.Test;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.entities.EntityConstants;
import step.core.reporting.model.ReportLayout;
import step.migration.MigrationContext;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class V31_0_ReportLayoutReportTypeMigrationTaskTest {

    @Test
    public void documentWithoutReportType_isBackfilledToSingleExecution() {
        InMemoryCollectionFactory cf = new InMemoryCollectionFactory(new Properties());
        Collection<Document> reportLayouts = cf.getCollection(EntityConstants.reportLayouts, Document.class);

        Document legacy = new Document();
        legacy.put("visibility", ReportLayout.ReportLayoutVisibility.Private.name());
        reportLayouts.save(legacy);

        runMigration(cf);

        Document result = findFirst(reportLayouts);
        assertEquals(ReportLayout.ReportLayoutType.SingleExecution.name(), result.getString(ReportLayout.FIELD_REPORT_TYPE));
    }

    @Test
    public void documentWithExistingReportType_isUntouched() {
        InMemoryCollectionFactory cf = new InMemoryCollectionFactory(new Properties());
        Collection<Document> reportLayouts = cf.getCollection(EntityConstants.reportLayouts, Document.class);

        Document crossExecution = new Document();
        crossExecution.put(ReportLayout.FIELD_REPORT_TYPE, ReportLayout.ReportLayoutType.CrossExecution.name());
        reportLayouts.save(crossExecution);

        runMigration(cf);

        Document result = findFirst(reportLayouts);
        assertEquals("Existing report type must not be overwritten",
            ReportLayout.ReportLayoutType.CrossExecution.name(), result.getString(ReportLayout.FIELD_REPORT_TYPE));
    }

    @Test
    public void mixedDocuments_onlyLegacyOnesAreBackfilled() {
        InMemoryCollectionFactory cf = new InMemoryCollectionFactory(new Properties());
        Collection<Document> reportLayouts = cf.getCollection(EntityConstants.reportLayouts, Document.class);

        Document legacy = new Document();
        legacy.put("name", "legacy");
        reportLayouts.save(legacy);

        Document cross = new Document();
        cross.put("name", "cross");
        cross.put(ReportLayout.FIELD_REPORT_TYPE, ReportLayout.ReportLayoutType.CrossExecution.name());
        reportLayouts.save(cross);

        runMigration(cf);

        List<Document> results = reportLayouts.find(Filters.empty(), null, null, null, 0).collect(Collectors.toList());
        assertEquals(2, results.size());
        Document migratedLegacy = results.stream().filter(d -> "legacy".equals(d.getString("name"))).findFirst().orElseThrow();
        Document untouchedCross = results.stream().filter(d -> "cross".equals(d.getString("name"))).findFirst().orElseThrow();
        assertEquals(ReportLayout.ReportLayoutType.SingleExecution.name(), migratedLegacy.getString(ReportLayout.FIELD_REPORT_TYPE));
        assertEquals(ReportLayout.ReportLayoutType.CrossExecution.name(), untouchedCross.getString(ReportLayout.FIELD_REPORT_TYPE));
    }

    private static void runMigration(InMemoryCollectionFactory cf) {
        new V31_0_ReportLayoutReportTypeMigrationTask(cf, new MigrationContext()).runUpgradeScript();
    }

    private static Document findFirst(Collection<Document> reportLayouts) {
        return reportLayouts.find(Filters.empty(), null, null, null, 0).findFirst()
            .orElseThrow(() -> new AssertionError("No document found"));
    }
}
