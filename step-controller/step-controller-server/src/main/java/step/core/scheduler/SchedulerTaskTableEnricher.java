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
            try {
                ExecutionScheduler scheduler = executionSchedulerSupplier.get();
                if (scheduler == null) {
                    task.setNextExecutionTimestamp(null);
                } else {
                    task.setNextExecutionTimestamp(scheduler.getNextExecutionDate(task));
                }
            } catch (RuntimeException e) {
                logger.error("Unable to compute next execution date for scheduler task {}", task.getId(), e);
                task.setNextExecutionTimestamp(null);
            }
        }
        return task;
    }
}
