package step.controller.services.bulk;

import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.deployment.ControllerServiceException;
import step.core.objectenricher.ObjectFilter;
import step.core.ql.OQLFilterBuilder;
import step.framework.server.tables.service.TableFilter;

import java.util.List;
import java.util.function.Consumer;

public class BulkOperationManager {

    private final AsyncTaskManager asyncTaskManager;

    public BulkOperationManager(AsyncTaskManager asyncTaskManager) {
        this.asyncTaskManager = asyncTaskManager;
    }

    public AsyncTaskStatus<Void> performBulkOperation(BulkOperationParameters parameters, Consumer<String> operationById, Consumer<Filter> operationByFilter, ObjectFilter contextObjectFilter) {
        BulkOperationTargetType targetType = parameters.getTargetType();
        validateParameters(parameters, targetType);
        if(parameters.isSimulate()) {
            throw new ControllerServiceException(400, "Simulate mode currently not supported");
        }
        return asyncTaskManager.scheduleAsyncTask(t -> {
            if (targetType == BulkOperationTargetType.LIST) {
                List<String> ids = parameters.getIds();
                if (ids != null && !ids.isEmpty()) {
                    ids.forEach(operationById);
                }
            } else if (targetType == BulkOperationTargetType.FILTER) {
                TableFilter tableFilter = parameters.getFilter();
                if (tableFilter != null) {
                    Filter requestFilter = tableFilter.toFilter();

                    Filter contextFilter = OQLFilterBuilder.getFilter(contextObjectFilter.getOQLFilter());

                    Filters.And filter = Filters.and(List.of(contextFilter, requestFilter));

                    operationByFilter.accept(filter);
                }
            } else if (targetType == BulkOperationTargetType.ALL) {
                Filter contextFilter = OQLFilterBuilder.getFilter(contextObjectFilter.getOQLFilter());

                operationByFilter.accept(contextFilter);
            }

            return null;
        });
    }

    private void validateParameters(BulkOperationParameters parameters, BulkOperationTargetType targetType) {
        if (targetType == BulkOperationTargetType.LIST) {
            List<String> ids = parameters.getIds();
            if (ids == null || ids.isEmpty()) {
                throw new ControllerServiceException(400, "No Ids specified. Please specify a list of entity Ids to be processed");
            }
        } else if (targetType == BulkOperationTargetType.FILTER) {
            TableFilter tableFilter = parameters.getFilter();
            if (tableFilter == null) {
                throw new ControllerServiceException(400, "No filter specified. Please specify filter for the entities to to be processed");
            }
        } else if (targetType == BulkOperationTargetType.ALL) {
            if(parameters.getFilter() != null || parameters.getIds() != null) {
                throw new ControllerServiceException(400, "No filter or Ids should be specified using target ALL.");
            }
        }
    }
}
