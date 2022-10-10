package step.controller.services.bulk;

import ch.exense.commons.io.Poller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.deployment.ControllerServiceException;
import step.core.entities.Bean;
import step.core.objectenricher.ObjectFilter;
import step.framework.server.tables.service.FieldFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BulkOperationManagerTest {

    private final InMemoryCollection<Bean> collection = new InMemoryCollection<>();
    private final Bean bean1 = new Bean("value1");
    private final Bean bean2 = new Bean("value2");
    private final BulkOperationManager<Bean> bulkOperationManager = new BulkOperationManager<>(collection, new AsyncTaskManager());
    private final ArrayList<String> ids = new ArrayList<>();

    @BeforeEach
    public void before() {
        collection.save(bean1);
        collection.save(bean2);
    }

    @Test
    void performBulkOperation() throws InterruptedException, TimeoutException {
        BulkOperationParameters parameters = new BulkOperationParameters();
        parameters.setTargetType(BulkOperationTargetType.LIST);
        assertThrows(ControllerServiceException.class, () -> bulkOperationManager.performBulkOperation(parameters, ids::add, () -> "", null));

        List<String> targetIds = List.of(bean1.getId().toString(), bean2.getId().toString());
        parameters.setIds(targetIds);
        AsyncTaskStatus<BulkOperationReport> exportStatus = bulkOperationManager.performBulkOperation(parameters, ids::add, () -> "", null);
        Poller.waitFor(exportStatus::isReady, 2000);
        assertEquals(ids, targetIds);
        assertEquals(2, exportStatus.getResult().getCount());
    }

    @Test
    void performBulkOperationPreview() throws InterruptedException, TimeoutException {
        BulkOperationParameters parameters = new BulkOperationParameters();
        parameters.setTargetType(BulkOperationTargetType.LIST);
        parameters.setPreview(true);

        parameters.setIds(List.of(bean1.getId().toString(), bean2.getId().toString()));
        AsyncTaskStatus<BulkOperationReport> exportStatus = bulkOperationManager.performBulkOperation(parameters, ids::add, () -> "", null);
        Poller.waitFor(exportStatus::isReady, 2000);
        // Ensure that no processing has been done in preview mode
        assertEquals(ids, List.of());
        assertEquals(2, exportStatus.getResult().getCount());
    }

    @Test
    void performBulkOperationFilter() throws InterruptedException, TimeoutException {
        BulkOperationParameters parameters = new BulkOperationParameters();
        parameters.setTargetType(BulkOperationTargetType.FILTER);

        assertThrows(ControllerServiceException.class, () -> bulkOperationManager.performBulkOperation(parameters, ids::add, () -> "", null));

        parameters.setFilter(new FieldFilter("property1", "value1", true));
        AsyncTaskStatus<BulkOperationReport> exportStatus = bulkOperationManager.performBulkOperation(parameters, ids::add, () -> "", null);
        Poller.waitFor(exportStatus::isReady, 2000);

        assertEquals(List.of(bean1.getId().toString()), ids);
        assertEquals(1, exportStatus.getResult().getCount());
    }

    @Test
    void performBulkOperationAll() throws InterruptedException, TimeoutException {
        BulkOperationParameters parameters = new BulkOperationParameters();
        parameters.setTargetType(BulkOperationTargetType.ALL);
        parameters.setIds(List.of());

        assertThrows(ControllerServiceException.class, () -> bulkOperationManager.performBulkOperation(parameters, ids::add, () -> "", null));

        parameters.setIds(null);
        parameters.setFilter(new FieldFilter());

        assertThrows(ControllerServiceException.class, () -> bulkOperationManager.performBulkOperation(parameters, ids::add, () -> "", null));

        parameters.setIds(null);
        parameters.setFilter(null);

        ObjectFilter objectFilter = () -> "property1 = 'value1'";
        AsyncTaskStatus<BulkOperationReport> exportStatus = bulkOperationManager.performBulkOperation(parameters, ids::add, objectFilter, null);
        Poller.waitFor(exportStatus::isReady, 2000);
        assertEquals(List.of(bean1.getId().toString()), ids);
        assertEquals(1, exportStatus.getResult().getCount());
    }
}