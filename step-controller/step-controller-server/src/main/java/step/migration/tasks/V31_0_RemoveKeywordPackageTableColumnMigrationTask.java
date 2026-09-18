package step.migration.tasks;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.core.entities.EntityConstants;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Removes what is left of the keyword package column on the keywords table: the
 * {@code functionTableExtensions} screen input and the column of every table settings document.
 * <p>
 * Both outlive the feature, and the screen input points at {@code rest/table/functionPackage}, which
 * no longer exists.
 */
public class V31_0_RemoveKeywordPackageTableColumnMigrationTask extends MigrationTask {

    private static final String FUNCTION_PACKAGE_ID_COLUMN = "customFields.functionPackageId";
    private static final String FUNCTION_TABLE_EXTENSIONS = "functionTableExtensions";
    private static final String COLUMN_SETTINGS_LIST = "columnSettingsList";

    public V31_0_RemoveKeywordPackageTableColumnMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3, 31, 0), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        removeScreenInput();
        removeTableSettingsColumn();
    }

    private void removeScreenInput() {
        Collection<Document> screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
        logger.info("Removing the keyword package screen input of '{}'.", FUNCTION_TABLE_EXTENSIONS);
        screenInputs.remove(Filters.and(List.of(Filters.equals("screenId", FUNCTION_TABLE_EXTENSIONS),
                Filters.equals("input.id", FUNCTION_PACKAGE_ID_COLUMN))));
    }

    /**
     * Every scope is visited, not only the system one, since a user who reordered the keywords table
     * owns a copy of the column list. The remaining positions are left as they are, the order they
     * define being unaffected by the gap.
     */
    private void removeTableSettingsColumn() {
        Collection<Document> tableSettings = collectionFactory.getCollection("tablesettings", Document.class);
        AtomicLong counter = new AtomicLong(0);
        try (Stream<Document> documents = tableSettings.findLazy(
                Filters.equals("scope.settingId", EntityConstants.functions), null, null, null, 0)) {
            documents.forEach(document -> {
                // getArray returns a copy, so the modified array has to be put back
                List<DocumentObject> columns = document.getArray(COLUMN_SETTINGS_LIST);
                if (columns != null && columns.removeIf(
                        column -> FUNCTION_PACKAGE_ID_COLUMN.equals(column.getString("columnId")))) {
                    document.put(COLUMN_SETTINGS_LIST, columns);
                    tableSettings.save(document);
                    counter.incrementAndGet();
                }
            });
        }
        logger.info("Removed the keyword package column from {} keywords table settings.", counter.get());
    }

}
