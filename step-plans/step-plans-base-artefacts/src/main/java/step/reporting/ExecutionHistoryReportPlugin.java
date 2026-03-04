package step.reporting;

import ch.exense.commons.app.Configuration;
import step.core.execution.ExecutionContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionResultSnapshot;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Plugin
public class ExecutionHistoryReportPlugin extends AbstractExecutionEnginePlugin {

    public static final String EXECUTIONS_HISTORY_COLLECT_COUNT = "executions.history.collectCount";

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        Configuration configuration = context.getConfiguration();
        Integer countItems = configuration.getPropertyAsInteger(EXECUTIONS_HISTORY_COLLECT_COUNT, 10);
        ExecutionAccessor executionAccessor = context.getExecutionAccessor();
        Execution execution = context.getExecutionManager().getExecution();
        long searchBeforeTimestamp = execution.getEndTime() != null ? execution.getEndTime() : System.currentTimeMillis();
        List<ExecutionResultSnapshot> pastExecutionsSnapshots = executionAccessor.getLastEndedExecutionsByCanonicalPlanName(execution.getImportResult().getCanonicalPlanName(), countItems, searchBeforeTimestamp, Set.of(execution.getId().toString()))
                .map(e -> {
                    ExecutionResultSnapshot snapshot = new ExecutionResultSnapshot();
                    snapshot.setId(e.getId().toString());
                    snapshot.setResult(e.getResult());
                    snapshot.setStatus(e.getStatus());
                    snapshot.setStartTime(e.getStartTime());
                    return snapshot;
                })
                .collect(Collectors.toList());
        execution.setHistoryResults(pastExecutionsSnapshots);
        executionAccessor.save(execution);
    }

}
