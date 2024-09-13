package step.core.artefacts.reports.aggregated;

public class ReportNodeTimeSeriesCollectionsSettings {

    private boolean perMinuteEnabled;
    private long perMinuteFlushInterval;
    private boolean hourlyEnabled;
    private long hourlyFlushInterval;
    private boolean dailyEnabled;
    private long dailyFlushInterval;
    private boolean weeklyEnabled;
    private long weeklyFlushInterval;

    public boolean isDailyEnabled() {
        return dailyEnabled;
    }

    public boolean isPerMinuteEnabled() {
        return perMinuteEnabled;
    }

    public boolean isHourlyEnabled() {
        return hourlyEnabled;
    }

    public boolean isWeeklyEnabled() {
        return weeklyEnabled;
    }


    public ReportNodeTimeSeriesCollectionsSettings setPerMinuteEnabled(boolean perMinuteEnabled) {
        this.perMinuteEnabled = perMinuteEnabled;
        return this;
    }

    public ReportNodeTimeSeriesCollectionsSettings setHourlyEnabled(boolean hourlyEnabled) {
        this.hourlyEnabled = hourlyEnabled;
        return this;
    }

    public ReportNodeTimeSeriesCollectionsSettings setDailyEnabled(boolean dailyEnabled) {
        this.dailyEnabled = dailyEnabled;
        return this;
    }

    public ReportNodeTimeSeriesCollectionsSettings setWeeklyEnabled(boolean weeklyEnabled) {
        this.weeklyEnabled = weeklyEnabled;
        return this;
    }

    public long getPerMinuteFlushInterval() {
        return perMinuteFlushInterval;
    }

    public ReportNodeTimeSeriesCollectionsSettings setPerMinuteFlushInterval(long perMinuteFlushInterval) {
        this.perMinuteFlushInterval = perMinuteFlushInterval;
        return this;
    }

    public long getHourlyFlushInterval() {
        return hourlyFlushInterval;
    }

    public ReportNodeTimeSeriesCollectionsSettings setHourlyFlushInterval(long hourlyFlushInterval) {
        this.hourlyFlushInterval = hourlyFlushInterval;
        return this;
    }

    public long getDailyFlushInterval() {
        return dailyFlushInterval;
    }

    public ReportNodeTimeSeriesCollectionsSettings setDailyFlushInterval(long dailyFlushInterval) {
        this.dailyFlushInterval = dailyFlushInterval;
        return this;
    }

    public long getWeeklyFlushInterval() {
        return weeklyFlushInterval;
    }

    public ReportNodeTimeSeriesCollectionsSettings setWeeklyFlushInterval(long weeklyFlushInterval) {
        this.weeklyFlushInterval = weeklyFlushInterval;
        return this;
    }

}
