package step.migration.tasks;

import com.mongodb.MongoException;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.postgresql.PostgreSQLCollection;
import step.core.entities.EntityConstants;
import step.migration.AsyncMigrationTask;
import step.migration.MigrationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class V30_0_ReportNodeAncestorIdsMigrationTask extends AsyncMigrationTask {

    public static final String PATH_FIELD_NAME = "path";
    public static final int OBJECT_ID_HEX_LENGTH = 24;

    private static final String TASK_NAME = V30_0_ReportNodeAncestorIdsMigrationTask.class.getSimpleName();
    public static final String ANCESTOR_IDS = "ancestorIds";
    private static final int BATCH_SIZE = 100;

    public V30_0_ReportNodeAncestorIdsMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3, 30, 0), collectionFactory, migrationContext);
    }

    @Override
    public void runAsyncUpgradeScript() {
        Collection<Document> reports = collectionFactory.getCollection(EntityConstants.reports, Document.class);
        cleanupIndexOnPath(reports);
        convertPathToAncestorIds(reports);
    }

    private void cleanupIndexOnPath(Collection<Document> reports) {
        //We start to have such recurring pattern and should add an interface to drop indexes by index specification and let the implementation rebuild the index name
        if (reports instanceof PostgreSQLCollection) {
            reports.dropIndex("idx_reports_executionidasc_pathasc");
        } else {
            try {
                reports.dropIndex("executionID_1_path_1");
            } catch (MongoException e) {
                logger.warn("Index executionID_1_path_1 on the reports collection could not be deleted, it probably doesn't exist anymore: {}", e.getMessage());
            }
        }
    }

    public static void convertPathToAncestorIds(Collection<Document> reports) {
        AtomicLong counter = new AtomicLong(0);
        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
            BasicThreadFactory.builder().namingPattern("ancestor-ids-migration-progress-%d").daemon(true).build())) {
            scheduler.scheduleAtFixedRate(
                () -> logger.info("[{}] {} nodes migrated so far...", TASK_NAME, counter.get()),
                1, 1, TimeUnit.MINUTES);
            logger.info("[{}] Starting migration of report node ancestor IDs...", TASK_NAME);
            List<Document> batch = new ArrayList<>(BATCH_SIZE);
            try (Stream<Document> documents = reports.findLazy(Filters.exists(PATH_FIELD_NAME), null, null, null, 0)) {
                documents.forEach(document -> {
                    Object pathObject = document.remove(PATH_FIELD_NAME);
                    if (pathObject instanceof String path) {
                        List<String> ancestorIds = new ArrayList<>();
                        for (int i = 0; i + OBJECT_ID_HEX_LENGTH <= path.length(); i += OBJECT_ID_HEX_LENGTH) {
                            ancestorIds.add(path.substring(i, i + OBJECT_ID_HEX_LENGTH));
                        }
                        if (!ancestorIds.isEmpty()) {
                            document.put(ANCESTOR_IDS, ancestorIds);
                        }
                    }
                    batch.add(document);
                    if (batch.size() == BATCH_SIZE) {
                        reports.save(batch);
                        counter.addAndGet(BATCH_SIZE);
                        batch.clear();
                    }
                });
            }
            if (!batch.isEmpty()) {
                reports.save(batch);
                counter.addAndGet(batch.size());
            }
        }
        logger.info("[{}] Completed: {} nodes migrated.", TASK_NAME, counter.get());
    }
}
