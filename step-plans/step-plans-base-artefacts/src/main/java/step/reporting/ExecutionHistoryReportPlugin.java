package step.reporting;

import ch.exense.commons.app.Configuration;
import step.core.execution.ExecutionContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionResultSnapshot;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.util.List;
import java.util.stream.Collectors;

@Plugin
public class ExecutionHistoryReportPlugin  extends AbstractExecutionEnginePlugin {

    public static final String EXECUTIONS_HISTORY_COLLECT_COUNT = "executions.history.collectCount";

    @Override
    public void executionStart(ExecutionContext context) {
        // TOOD collect the executions here too
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        Configuration configuration = context.getConfiguration();
        Integer countItems = configuration.getPropertyAsInteger(EXECUTIONS_HISTORY_COLLECT_COUNT, 10);
        ExecutionAccessor executionAccessor = context.getExecutionAccessor();
        Execution execution = context.getExecutionManager().getExecution();
        // endTime is not set here
        long searchBeforeTimestamp = execution.getEndTime() != null ? execution.getEndTime() : System.currentTimeMillis() - 1;
        List<ExecutionResultSnapshot> pastExecutionsSnapshots = executionAccessor.getLastEndedExecutionsByCanonicalPlanName(execution.getCanonicalPlanName(), countItems, searchBeforeTimestamp)
                                .map(e -> new ExecutionResultSnapshot()
                                        .setId(e.getId().toString())
                                        .setResult(e.getResult())
                                        .setStatus(e.getStatus())
                                        .setStartTime(e.getStartTime()))
                                .collect(Collectors.toList());
        execution.setHistoryResults(pastExecutionsSnapshots);
        executionAccessor.save(execution);
    }

}
