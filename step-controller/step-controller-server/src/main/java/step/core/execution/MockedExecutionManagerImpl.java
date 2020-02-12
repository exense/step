package step.core.execution;

import java.util.Map;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.ImportResult;

public class MockedExecutionManagerImpl implements ExecutionManager {

	@Override
	public void updateExecutionType(ExecutionContext context, String newType) {
	}

	@Override
	public void updateExecutionResult(ExecutionContext context, ReportNodeStatus resultStatus) {
	}

	@Override
	public void updateStatus(ExecutionContext context, ExecutionStatus status) {
	}

	@Override
	public void persistImportResult(ExecutionContext context, ImportResult importResult) {
	}

	@Override
	public void persistStatus(ExecutionContext context) {
	}

	@Override
	public void updateParameters(ExecutionContext context, Map<String, String> params) {
	}
}
