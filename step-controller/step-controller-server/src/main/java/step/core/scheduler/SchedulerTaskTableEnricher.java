package step.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.objectenricher.TriFunction;
import step.framework.server.Session;
import step.framework.server.tables.service.TableParameters;

import java.util.function.Supplier;

public class SchedulerTaskTableEnricher implements TriFunction<ExecutiontTaskParameters, Session<?>, TableParameters, ExecutiontTaskParameters> {

    public static final String NEXT_EXECUTION_TIMESTAMP = "nextExecutionTimestamp";

    private static final Logger logger = LoggerFactory.getLogger(SchedulerTaskTableEnricher.class);

    private final Supplier<ExecutionScheduler> executionSchedulerSupplier;

    public SchedulerTaskTableEnricher(Supplier<ExecutionScheduler> executionSchedulerSupplier) {
        this.executionSchedulerSupplier = executionSchedulerSupplier;
    }

    @Override
    public ExecutiontTaskParameters apply(ExecutiontTaskParameters task, Session<?> session, TableParameters tableParameters) {
        if (task != null) {
            try {
                ExecutionScheduler scheduler = executionSchedulerSupplier.get();
                Long nextExecutionDate = scheduler == null ? null : scheduler.getNextExecutionDate(task.getId().toString());
                task.addAttribute(NEXT_EXECUTION_TIMESTAMP, nextExecutionDate == null ? null : nextExecutionDate.toString());
            } catch (RuntimeException e) {
                logger.warn("Unable to compute next execution date for scheduler task {}", task.getId(), e);
                task.addAttribute(NEXT_EXECUTION_TIMESTAMP, null);
            }
        }
        return task;
    }
}
