package step.engine.execution;

import java.util.Map;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionTypeListener;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.ImportResult;

public interface ExecutionManager extends ExecutionTypeListener {

	void updateExecutionType(ExecutionContext context, String newType);

	void updateExecutionResult(ExecutionContext context, ReportNodeStatus resultStatus);

	void updateStatus(ExecutionContext context, ExecutionStatus status);

	void persistImportResult(ExecutionContext context, ImportResult importResult);

	void persistStatus(ExecutionContext context);

	void updateParameters(ExecutionContext context, Map<String, String> params);

}