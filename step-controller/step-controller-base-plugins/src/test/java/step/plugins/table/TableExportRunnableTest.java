package step.plugins.table;

import org.junit.Test;
import step.controller.services.async.AsyncTaskHandle;
import step.controller.services.async.AsyncTaskStatus;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.entities.SimpleBean;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.framework.server.tables.service.TableRequest;
import step.framework.server.tables.service.TableService;
import step.resources.LocalResourceManagerImpl;
import step.resources.Resource;
import step.resources.ResourceRevisionFileHandle;

import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TableExportRunnableTest {

    @Test
    public void runExport() throws Exception {
        TableRegistry tableRegistry = new TableRegistry();
        InMemoryCollection<SimpleBean> collection = new InMemoryCollection<>();
        collection.save(new SimpleBean("property 1"));
        collection.save(new SimpleBean("property 2"));
        tableRegistry.register("test", new Table<>(collection, null, false));
        TableService tableService = new TableService(tableRegistry, new ObjectHookRegistry(), null);
        TableExportRequest exportRequest = new TableExportRequest();
        exportRequest.setTableRequest(new TableRequest());
        exportRequest.setFields(List.of("stringProperty"));
        LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
        TableExportTask exportRunnable = new TableExportTask(tableService, resourceManager, "test", exportRequest, null);
        Resource resource = exportRunnable.apply(new AsyncTaskHandle(new AsyncTaskStatus<>()));
        ResourceRevisionFileHandle resourceFile = resourceManager.getResourceFile(resource.getId().toString());
        List<String> allLines = Files.readAllLines(resourceFile.getResourceFile().toPath());
        assertEquals(List.of("stringProperty;", "property 1;", "property 2;"), allLines);
    }
}