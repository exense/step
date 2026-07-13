package step.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.objectenricher.TriFunction;
import step.framework.server.Session;
import step.framework.server.tables.service.TableParameters;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

public class SchedulerTaskTableEnricher implements TriFunction<ExecutiontTaskParameters, Session<?>, TableParameters, ExecutiontTaskParameters> {

    public static final String NEXT_EXECUTION_TIMESTAMP = "nextExecutionTimestamp";

    private static final Logger logger = LoggerFactory.getLogger(SchedulerTaskTableEnricher.class);

    @Override
    public ExecutiontTaskParameters apply(ExecutiontTaskParameters task, Session<?> session, TableParameters tableParameters) {
        if (task != null) {
            try {
                List<CronExclusion> cronExclusions = task.getCronExclusions();
                Long nextExecutionDate = CronUtils.getNextExecutionDate(
                    task.getCronExpression(),
                    cronExclusions == null ? null : cronExclusions.stream().map(CronExclusion::getCronExpression).collect(Collectors.toList()));
                task.addAttribute(NEXT_EXECUTION_TIMESTAMP, nextExecutionDate == null ? null : nextExecutionDate.toString());
            } catch (ParseException | RuntimeException e) {
                logger.warn("Unable to compute next execution date for scheduler task {}", task.getId(), e);
                task.addAttribute(NEXT_EXECUTION_TIMESTAMP, null);
            }
        }
        return task;
    }
}
