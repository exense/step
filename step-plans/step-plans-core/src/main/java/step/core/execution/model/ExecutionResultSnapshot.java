package step.core.execution.model;

import step.core.artefacts.reports.ReportNodeStatus;

public class ExecutionResultSnapshot {

    private String id;
    private ExecutionStatus status;
    private ReportNodeStatus result;

    public String getId() {
        return id;
    }

    public ExecutionResultSnapshot setId(String id) {
        this.id = id;
        return this;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public ExecutionResultSnapshot setStatus(ExecutionStatus status) {
        this.status = status;
        return this;
    }

    public ReportNodeStatus getResult() {
        return result;
    }

    public ExecutionResultSnapshot setResult(ReportNodeStatus result) {
        this.result = result;
        return this;
    }
}
