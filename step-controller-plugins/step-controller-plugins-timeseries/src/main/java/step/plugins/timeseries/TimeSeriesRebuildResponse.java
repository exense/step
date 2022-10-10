package step.plugins.timeseries;

public class TimeSeriesRebuildResponse {

    private long numberOfMeasurementsProcessed;

    public TimeSeriesRebuildResponse(long numberOfMeasurementsProcessed) {
        this.numberOfMeasurementsProcessed = numberOfMeasurementsProcessed;
    }

    public long getNumberOfMeasurementsProcessed() {
        return numberOfMeasurementsProcessed;
    }
}
