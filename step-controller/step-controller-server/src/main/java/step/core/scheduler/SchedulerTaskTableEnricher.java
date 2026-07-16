package step.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.objectenricher.TriFunction;
import step.framework.server.Session;
import step.framework.server.tables.service.TableParameters;

import java.util.function.Supplier;

public class SchedulerTaskTableEnricher implements TriFunction<SchedulerTaskWrapper, Session<?>, TableParameters, SchedulerTaskWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerTaskTableEnricher.class);

    private final Supplier<ExecutionScheduler> executionSchedulerSupplier;

    public SchedulerTaskTableEnricher(Supplier<ExecutionScheduler> executionSchedulerSupplier) {
        this.executionSchedulerSupplier = executionSchedulerSupplier;
    }

    @Override
    public SchedulerTaskWrapper apply(SchedulerTaskWrapper task, Session<?> session, TableParameters tableParameters) {
        if (task != null) {
            String cronExpression = task.getCronExpression();
            if (!task.isActive() || cronExpression == null || cronExpression.isEmpty()) {
                task.setNextExecutionTimestamp(null);
            } else {
                try {
                    ExecutionScheduler scheduler = executionSchedulerSupplier.get();
                    Long nextExecutionDate = scheduler == null ? null : scheduler.getNextExecutionDate(task.getId().toString());
                    task.setNextExecutionTimestamp(nextExecutionDate);
                } catch (RuntimeException e) {
                    logger.error("Unable to compute next execution date for scheduler task {}", task.getId(), e);
                    task.setNextExecutionTimestamp(null);
                }
            }
        }
        return task;
    }
}
