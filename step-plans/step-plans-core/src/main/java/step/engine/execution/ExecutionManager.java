package step.engine.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionStatus;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExecutionManager {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionManager.class);
    private final ExecutionContext executionContext;

    public ExecutionManager(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public Execution getExecution() {
        ExecutionAccessor executionAccessor = executionContext.getExecutionAccessor();
        return executionAccessor.get(executionContext.getExecutionId());
    }

    public void updateExecutionType(String newType) {
        executionContext.setExecutionType(newType);
        updateExecution(e -> {
            e.setExecutionType(newType);
        });
    }

    public void updateStatus(ExecutionStatus newStatus) {
        executionContext.updateStatus(newStatus);
        updateExecution(execution->{
            if (newStatus == ExecutionStatus.ENDED) {
                execution.setEndTime(System.currentTimeMillis());
                Stream<ReportNode> reportNodes = executionContext.getReportNodeAccessor().getReportNodesByExecutionID(executionContext.getExecutionId());
                String agentsInvolved = getAgentsInvolvedAsJoinedString(reportNodes);
                execution.setAgentsInvolved(agentsInvolved);
            }
            execution.setStatus(newStatus);
        });
    }

    private String getAgentsInvolvedAsJoinedString(Stream<ReportNode> reportNodes) {
        return reportNodes
                .map(this::getAgentUrlFromReportNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream().sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(" "));
    }

    private String getAgentUrlFromReportNode(ReportNode reportNode) {
        // we know that only CallFunctionReportNodes contain agent information, but we don't have access to the class definition here.
        if (reportNode.getClass().getName().equals("step.artefacts.reports.CallFunctionReportNode")) {
            try {
                Method getter = reportNode.getClass().getMethod("getAgentUrl");
                return (String) getter.invoke(reportNode);
            } catch (Exception e) {
                // should never happen
                logger.error(e.getMessage(), e);
                return null;
            }
        }
        return null;
    }

    public void updateExecution(Consumer<Execution> consumer) {
        ExecutionAccessor executionAccessor = executionContext.getExecutionAccessor();
        String executionId = executionContext.getExecutionId();
        Execution execution = executionAccessor.get(executionId);
        if (execution != null) {
            consumer.accept(execution);
            executionAccessor.save(execution);
        } else {
            logger.warn("Unable to update execution. No execution found for id: " + executionId);
        }
    }
}
