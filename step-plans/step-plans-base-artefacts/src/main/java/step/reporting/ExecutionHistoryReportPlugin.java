package step.reporting;

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

    @Override
    public void executionStart(ExecutionContext context) {
        // TOOD collect the executions here too
    }

    @Override
    public void afterExecutionEnd(ExecutionContext context) {
        ExecutionAccessor executionAccessor = context.getExecutionAccessor();
        Execution execution = context.getExecutionManager().getExecution();
        // endTime is not set here
        long searchBeforeTimestamp = System.currentTimeMillis() - 1;
        List<ExecutionResultSnapshot> pastExecutionsSnapshots = executionAccessor.getLastEndedExecutionsByCanonicalPlanName(execution.getCanonicalPlanName(), 10, searchBeforeTimestamp)
                        .stream()
                                .map(e -> new ExecutionResultSnapshot()
                                        .setId(e.getId().toString())
                                        .setResult(e.getResult())
                                        .setStatus(e.getStatus())
                                )
                                        .collect(Collectors.toList());
        execution.setHistoryResults(pastExecutionsSnapshots);
        executionAccessor.save(execution);
    }

}
