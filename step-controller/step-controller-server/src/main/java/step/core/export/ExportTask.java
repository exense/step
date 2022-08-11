package step.core.export;

import step.resources.Resource;

@FunctionalInterface
public interface ExportTask {
    Resource apply(ExportTaskManager.ExportTaskHandle t) throws Exception;
}
