package step.controller.services.bulk;

public class BulkOperationReport {

    private long count;

    public BulkOperationReport() {
    }

    public BulkOperationReport(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
