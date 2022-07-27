package step.plugins.table;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.entities.SimpleBean;
import step.core.export.ExportStatus;
import step.core.export.ExportTaskManager;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.tables.AbstractTable;
import step.framework.server.tables.TableRegistry;
import step.framework.server.tables.service.TableRequest;
import step.framework.server.tables.service.TableService;
import step.resources.LocalResourceManagerImpl;
import step.resources.Resource;
import step.resources.ResourceRevisionFileHandle;

import java.nio.file.Files;
import java.util.List;

class TableExportRunnableTest {

    @Test
    void runExport() throws Exception {
        TableRegistry tableRegistry = new TableRegistry();
        InMemoryCollection<SimpleBean> collection = new InMemoryCollection<>();
        collection.save(new SimpleBean("property 1"));
        collection.save(new SimpleBean("property 2"));
        tableRegistry.register("test", new AbstractTable<>(collection, false));
        TableService tableService = new TableService(tableRegistry, new ObjectHookRegistry());
        TableExportRequest exportRequest = new TableExportRequest();
        exportRequest.setTableRequest(new TableRequest());
        exportRequest.setFields(List.of("stringProperty"));
        TableExportTask exportRunnable = new TableExportTask(tableService, "test", exportRequest, null);
        LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
        Resource resource = exportRunnable.apply(new ExportTaskManager.ExportTaskHandle(resourceManager, new ExportStatus()));
        ResourceRevisionFileHandle resourceFile = resourceManager.getResourceFile(resource.getId().toString());
        List<String> allLines = Files.readAllLines(resourceFile.getResourceFile().toPath());
        Assertions.assertEquals(List.of("stringProperty;", "property 1;", "property 2;"), allLines);
    }
}