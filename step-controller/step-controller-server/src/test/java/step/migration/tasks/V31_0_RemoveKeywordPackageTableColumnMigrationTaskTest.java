package step.migration.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.migration.MigrationContext;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class V31_0_RemoveKeywordPackageTableColumnMigrationTaskTest {

    private InMemoryCollectionFactory collectionFactory;
    private Collection<Document> screenInputs;
    private Collection<Document> tableSettings;

    @Before
    public void before() throws Exception {
        collectionFactory = new InMemoryCollectionFactory(null);
        screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
        tableSettings = collectionFactory.getCollection("tablesettings", Document.class);

        save(screenInputs, "{ \"screenId\": \"functionTableExtensions\", \"input\": { \"id\": \"customFields.functionPackageId\" } }");
        save(screenInputs, "{ \"screenId\": \"functionTableExtensions\", \"input\": { \"id\": \"customFields.other\" } }");
        save(screenInputs, "{ \"screenId\": \"planTable\", \"input\": { \"id\": \"customFields.functionPackageId\" } }");

        // the system settings, plus a copy owned by a user who reordered the table
        save(tableSettings, "{ \"scope\": { \"settingId\": \"functions\" }, \"columnSettingsList\": ["
                + "{ \"columnId\": \"attributes.name\", \"visible\": true, \"position\": 0 },"
                + "{ \"columnId\": \"customFields.functionPackageId\", \"visible\": true, \"position\": 1 },"
                + "{ \"columnId\": \"actions\", \"visible\": true, \"position\": 2 } ] }");
        save(tableSettings, "{ \"scope\": { \"settingId\": \"functions\", \"user\": \"alice\" }, \"columnSettingsList\": ["
                + "{ \"columnId\": \"customFields.functionPackageId\", \"visible\": false, \"position\": 0 } ] }");
        save(tableSettings, "{ \"scope\": { \"settingId\": \"plans\" }, \"columnSettingsList\": ["
                + "{ \"columnId\": \"customFields.functionPackageId\", \"visible\": true, \"position\": 0 } ] }");
    }

    @Test
    public void theKeywordPackageScreenInputAndColumnsAreRemoved() {
        run();

        assertEquals("only the keyword package input of the keywords table is removed",
                2, screenInputs.count(Filters.empty(), null));
        assertEquals(0, screenInputs.count(Filters.and(List.of(
                Filters.equals("screenId", "functionTableExtensions"),
                Filters.equals("input.id", "customFields.functionPackageId"))), null));

        assertEquals(List.of("attributes.name", "actions"), columnsOf("functions", null));
        assertTrue("a user owned copy is cleaned up too", columnsOf("functions", "alice").isEmpty());
        assertEquals("a table other than the keywords one is untouched",
                List.of("customFields.functionPackageId"), columnsOf("plans", null));
    }

    @Test
    public void aSecondRunChangesNothing() {
        run();
        run();

        assertEquals(2, screenInputs.count(Filters.empty(), null));
        assertEquals(List.of("attributes.name", "actions"), columnsOf("functions", null));
    }

    private void run() {
        new V31_0_RemoveKeywordPackageTableColumnMigrationTask(collectionFactory, new MigrationContext())
                .runUpgradeScript();
    }

    private List<String> columnsOf(String settingId, String user) {
        Document document = tableSettings.find(user == null
                        ? Filters.and(List.of(Filters.equals("scope.settingId", settingId),
                            Filters.not(Filters.exists("scope.user"))))
                        : Filters.equals("scope.user", user),
                null, null, null, 0).findFirst().orElseThrow();
        return document.getArray("columnSettingsList").stream()
                .map(column -> column.getString("columnId")).collect(Collectors.toList());
    }

    private void save(Collection<Document> collection, String json) throws Exception {
        collection.save(new ObjectMapper().readValue(json, Document.class));
    }
}
