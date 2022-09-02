package step.controller.services.bulk;

import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.controller.services.entities.AbstractEntityServices;
import step.core.access.User;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.deployment.ControllerServiceException;
import step.core.objectenricher.ObjectFilter;
import step.core.ql.OQLFilterBuilder;
import step.framework.server.Session;
import step.framework.server.tables.service.TableFilter;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public class BulkOperationManager<T extends AbstractIdentifiableObject> {

    private final Collection<T> collection;
    private final AsyncTaskManager asyncTaskManager;

    public BulkOperationManager(Collection<T> collection, AsyncTaskManager asyncTaskManager) {
        this.collection = collection;
        this.asyncTaskManager = asyncTaskManager;
    }

    public AsyncTaskStatus<BulkOperationReport> performBulkOperation(BulkOperationParameters parameters, Consumer<String> operationById, ObjectFilter contextObjectFilter, Session<User> session) {
        BulkOperationTargetType targetType = parameters.getTargetType();
        validateParameters(parameters, targetType);

        LongAdder count = new LongAdder();
        Consumer<String> operationById_;
        if (parameters.isPreview()) {
            operationById_ = s -> count.increment();
        } else {
            operationById_ = s -> {
                count.increment();
                operationById.accept(s);
            };
        }

        return asyncTaskManager.scheduleAsyncTask(t -> {
            AbstractEntityServices.setCurrentSession(session);
            try {
                if (targetType == BulkOperationTargetType.LIST) {
                    List<String> ids = parameters.getIds();
                    if (ids != null && !ids.isEmpty()) {
                        ids.forEach(operationById_);
                    }
                } else {
                    Consumer<Filter> operationByFilter = getFilterConsumer(collection, operationById_);
                    Filter filter;
                    if (targetType == BulkOperationTargetType.FILTER) {
                        TableFilter tableFilter = parameters.getFilter();
                        if (tableFilter != null) {
                            Filter requestFilter = tableFilter.toFilter();
                            Filter contextFilter = OQLFilterBuilder.getFilter(contextObjectFilter.getOQLFilter());
                            filter = Filters.and(List.of(contextFilter, requestFilter));
                        } else {
                            throw new RuntimeException("Filter is null");
                        }
                    } else if (targetType == BulkOperationTargetType.ALL) {
                        filter = OQLFilterBuilder.getFilter(contextObjectFilter.getOQLFilter());
                    } else {
                        throw new RuntimeException("Unsupported targetFilter" + targetType);
                    }
                    operationByFilter.accept(filter);
                }

                return new BulkOperationReport(count.longValue());
            } finally {
                AbstractEntityServices.setCurrentSession(null);
            }
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
            if (parameters.getFilter() != null || parameters.getIds() != null) {
                throw new ControllerServiceException(400, "No filter or Ids should be specified using target ALL.");
            }
        }
    }

    private Consumer<Filter> getFilterConsumer(Collection<T> collection, Consumer<String> action) {
        return filter -> collection.find(filter, null, null, null, 0)
                .map(e -> e.getId().toString())
                .forEach(action);
    }
}
