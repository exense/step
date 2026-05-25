package step.migration.tasks;

import org.bson.types.ObjectId;
import org.junit.Test;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.entities.EntityConstants;
import step.migration.MigrationContext;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class V30_0_ReportNodeAncestorIdsMigrationTaskTest {

    private static final String ID_1 = new ObjectId().toHexString(); // 24 chars
    private static final String ID_2 = new ObjectId().toHexString();
    private static final String ID_3 = new ObjectId().toHexString();

    @Test
    public void testPathWithMultipleAncestors() {
        InMemoryCollectionFactory cf = new InMemoryCollectionFactory(new Properties());
        Collection<Document> reports = cf.getCollection(EntityConstants.reports, Document.class);

        // Three concatenated ObjectId hex strings
        Document doc = new Document();
        doc.put("path", ID_1 + ID_2 + ID_3);
        reports.save(doc);

        runMigration(cf);

        Document result = findFirst(reports);
        assertNull("path field must be removed", result.getString("path"));
        List<?> ancestorIds = (List<?>) result.get("ancestorIds");
        assertNotNull(ancestorIds);
        assertEquals(3, ancestorIds.size());
        assertEquals(ID_1, ancestorIds.get(0));
        assertEquals(ID_2, ancestorIds.get(1));
        assertEquals(ID_3, ancestorIds.get(2));
    }

    @Test
    public void testPathWithSingleAncestor() {
        InMemoryCollectionFactory cf = new InMemoryCollectionFactory(new Properties());
        Collection<Document> reports = cf.getCollection(EntityConstants.reports, Document.class);

        Document doc = new Document();
        doc.put("path", ID_1);
        reports.save(doc);

        runMigration(cf);

        Document result = findFirst(reports);
        assertNull(result.getString("path"));
        List<?> ancestorIds = (List<?>) result.get("ancestorIds");
        assertNotNull(ancestorIds);
        assertEquals(1, ancestorIds.size());
        assertEquals(ID_1, ancestorIds.get(0));
    }

    @Test
    public void testEmptyPathRemovesFieldWithoutAddingAncestorIds() {
        InMemoryCollectionFactory cf = new InMemoryCollectionFactory(new Properties());
        Collection<Document> reports = cf.getCollection(EntityConstants.reports, Document.class);

        Document doc = new Document();
        doc.put("path", "");
        reports.save(doc);

        runMigration(cf);

        Document result = findFirst(reports);
        assertNull("path field must be removed", result.getString("path"));
        assertNull("ancestorIds must not be set for an empty path", result.get("ancestorIds"));
    }

    @Test
    public void testDocumentWithoutPathIsUntouched() {
        InMemoryCollectionFactory cf = new InMemoryCollectionFactory(new Properties());
        Collection<Document> reports = cf.getCollection(EntityConstants.reports, Document.class);

        Document doc = new Document();
        doc.put("name", "no-path-doc");
        reports.save(doc);

        runMigration(cf);

        // findLazy(Filters.exists("path")) must not touch this document
        Document result = findFirst(reports);
        assertEquals("no-path-doc", result.getString("name"));
        assertNull(result.get("ancestorIds"));
    }

    @Test
    public void testMixedDocuments() {
        InMemoryCollectionFactory cf = new InMemoryCollectionFactory(new Properties());
        Collection<Document> reports = cf.getCollection(EntityConstants.reports, Document.class);

        Document withPath = new Document();
        withPath.put("path", ID_1 + ID_2);
        reports.save(withPath);

        Document withoutPath = new Document();
        withoutPath.put("name", "no-path");
        reports.save(withoutPath);

        runMigration(cf);

        List<Document> results = reports.find(Filters.empty(), null, null, null, 0).collect(Collectors.toList());
        assertEquals(2, results.size());

        Document migrated = results.stream().filter(d -> d.get("ancestorIds") != null).findFirst()
                .orElseThrow(() -> new AssertionError("No migrated document found"));
        assertNull(migrated.getString("path"));
        List<?> ancestorIds = (List<?>) migrated.get("ancestorIds");
        assertEquals(2, ancestorIds.size());
        assertEquals(ID_1, ancestorIds.get(0));
        assertEquals(ID_2, ancestorIds.get(1));

        Document untouched = results.stream().filter(d -> "no-path".equals(d.getString("name"))).findFirst()
                .orElseThrow(() -> new AssertionError("Untouched document not found"));
        assertNull(untouched.get("ancestorIds"));
    }

    private static void runMigration(InMemoryCollectionFactory cf) {
        new V30_0_ReportNodeAncestorIdsMigrationTask(cf, new MigrationContext()).runAsyncUpgradeScript();
    }

    private static Document findFirst(Collection<Document> reports) {
        return reports.find(Filters.empty(), null, null, null, 0).findFirst()
                .orElseThrow(() -> new AssertionError("No document found"));
    }
}