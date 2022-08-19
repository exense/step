package step.controller.services.bulk;

import ch.exense.commons.io.Poller;
import org.junit.jupiter.api.Test;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.deployment.ControllerServiceException;
import step.core.export.ExportStatus;
import step.core.export.ExportTaskManager;
import step.core.objectenricher.ObjectFilter;
import step.framework.server.tables.service.FieldFilter;
import step.resources.LocalResourceManagerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BulkOperationManagerTest {

    private BulkOperationManager bulkOperationManager = new BulkOperationManager(new ExportTaskManager(new LocalResourceManagerImpl()));
    private ArrayList<String> ids = new ArrayList<>();
    private ArrayList<Filter> filters = new ArrayList<>();

    @Test
    void performBulkOperation() throws InterruptedException, TimeoutException {
        BulkOperationParameters parameters = new BulkOperationParameters();
        parameters.setTargetType(BulkOperationTargetType.LIST);
        assertThrows(ControllerServiceException.class, () -> bulkOperationManager.performBulkOperation(parameters, ids::add, filters::add, () -> ""));

        List<String> targetIds = List.of("id1", "id2");
        parameters.setIds(targetIds);
        ExportStatus exportStatus = bulkOperationManager.performBulkOperation(parameters, ids::add, filters::add, () -> "");
        Poller.waitFor(() -> exportStatus.isReady(), 2000);
        assertEquals(ids, targetIds);
    }

    @Test
    void performBulkOperationFilter() throws InterruptedException, TimeoutException {
        BulkOperationParameters parameters = new BulkOperationParameters();
        parameters.setTargetType(BulkOperationTargetType.FILTER);

        assertThrows(ControllerServiceException.class, () -> bulkOperationManager.performBulkOperation(parameters, ids::add, filters::add, () -> ""));

        parameters.setFilter(new FieldFilter("field", "value", true));
        ExportStatus exportStatus = bulkOperationManager.performBulkOperation(parameters, ids::add, filters::add, () -> "");
        Poller.waitFor(() -> exportStatus.isReady(), 2000);
        assertEquals(Filters.And.class, filters.get(0).getClass());
    }

    @Test
    void performBulkOperationAll() throws InterruptedException, TimeoutException {
        BulkOperationParameters parameters = new BulkOperationParameters();
        parameters.setTargetType(BulkOperationTargetType.ALL);
        parameters.setIds(List.of());

        assertThrows(ControllerServiceException.class, () -> bulkOperationManager.performBulkOperation(parameters, ids::add, filters::add, () -> ""));

        parameters.setIds(null);
        parameters.setFilter(new FieldFilter());

        assertThrows(ControllerServiceException.class, () -> bulkOperationManager.performBulkOperation(parameters, ids::add, filters::add, () -> ""));

        parameters.setIds(null);
        parameters.setFilter(null);

        ObjectFilter objectFilter = () -> "field = 'value'";
        ExportStatus exportStatus = bulkOperationManager.performBulkOperation(parameters, ids::add, filters::add, objectFilter);
        Poller.waitFor(() -> exportStatus.isReady(), 2000);
        Filter actualFilter = filters.get(0);
        assertEquals(Filters.Equals.class, actualFilter.getClass());
    }
}