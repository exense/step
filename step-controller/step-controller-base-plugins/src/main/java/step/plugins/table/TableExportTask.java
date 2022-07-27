package step.plugins.table;

import org.apache.commons.beanutils.PropertyUtils;
import step.core.export.ExportTask;
import step.core.export.ExportTaskManager;
import step.framework.server.Session;
import step.framework.server.tables.service.TableResponse;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class TableExportTask implements ExportTask {

    private static final String END_OF_LINE = "\n";
    private static final String DELIMITER = ";";
    private final TableExportRequest exportRequest;
    private final String tableName;
    private final step.framework.server.tables.service.TableService tableService;
    private final Session session;

    public TableExportTask(step.framework.server.tables.service.TableService tableService, String tableName, TableExportRequest exportRequest, Session session) {
        this.tableService = tableService;
        this.tableName = tableName;
        this.exportRequest = exportRequest;
        this.session = session;
    }

    @Override
    public Resource apply(ExportTaskManager.ExportTaskHandle exportTaskHandle) throws Exception {
        ResourceRevisionContainer resourceContainer = exportTaskHandle.getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, "export.csv");
        PrintWriter writer = new PrintWriter(resourceContainer.getOutputStream());

        List<String> fields = exportRequest.getFields();

        // Write headers
        fields.forEach(field -> writer.append(field).append(DELIMITER));
        writer.append(END_OF_LINE);
        try {
            TableResponse<?> response = tableService.request(tableName, exportRequest.getTableRequest(), session);
            response.getData().forEach(o -> {
                // Write row
                fields.forEach(field -> {
                    Object property;
                    String formattedValue;
                    try {
                        property = PropertyUtils.getProperty(o, field);
                        if (property != null) {
                            formattedValue = property.toString();
                        } else {
                            formattedValue = "";
                        }
                    } catch (NoSuchMethodException e) {
                        formattedValue = "";
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Error while writing column " + field, e);
                    }
                    writer.append(formattedValue).append(DELIMITER);
                });
                writer.append(END_OF_LINE);
            });
        } finally {
            writer.close();
            resourceContainer.save(null);
        }

        return resourceContainer.getResource();
    }
}
