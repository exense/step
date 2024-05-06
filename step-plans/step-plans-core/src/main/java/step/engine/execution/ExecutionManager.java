package step.engine.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionStatus;

import java.util.function.Consumer;

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
            }
            execution.setStatus(newStatus);
        });
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
