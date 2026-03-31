package step.core.execution.model;

import step.core.artefacts.reports.ReportNodeStatus;

public class ExecutionResultSnapshot {

    private String id;
    private ExecutionStatus status;
    private ReportNodeStatus result;
    private long startTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public ReportNodeStatus getResult() {
        return result;
    }

    public void setResult(ReportNodeStatus result) {
        this.result = result;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
