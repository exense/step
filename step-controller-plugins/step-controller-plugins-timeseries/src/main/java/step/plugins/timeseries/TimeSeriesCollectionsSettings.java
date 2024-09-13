package step.plugins.timeseries;

public class TimeSeriesCollectionsSettings {

    private long mainCollectionResolution;
    private long mainCollectionFlushPeriod;
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

    public TimeSeriesCollectionsSettings setMainCollectionResolution(long mainCollectionResolution) {
        this.mainCollectionResolution = mainCollectionResolution;
        return this;
    }

    public TimeSeriesCollectionsSettings setMainCollectionFlushPeriod(long mainCollectionFlushPeriod) {
        this.mainCollectionFlushPeriod = mainCollectionFlushPeriod;
        return this;
    }

    public TimeSeriesCollectionsSettings setPerMinuteEnabled(boolean perMinuteEnabled) {
        this.perMinuteEnabled = perMinuteEnabled;
        return this;
    }

    public TimeSeriesCollectionsSettings setHourlyEnabled(boolean hourlyEnabled) {
        this.hourlyEnabled = hourlyEnabled;
        return this;
    }

    public TimeSeriesCollectionsSettings setDailyEnabled(boolean dailyEnabled) {
        this.dailyEnabled = dailyEnabled;
        return this;
    }

    public TimeSeriesCollectionsSettings setWeeklyEnabled(boolean weeklyEnabled) {
        this.weeklyEnabled = weeklyEnabled;
        return this;
    }

    public long getPerMinuteFlushInterval() {
        return perMinuteFlushInterval;
    }

    public TimeSeriesCollectionsSettings setPerMinuteFlushInterval(long perMinuteFlushInterval) {
        this.perMinuteFlushInterval = perMinuteFlushInterval;
        return this;
    }

    public long getHourlyFlushInterval() {
        return hourlyFlushInterval;
    }

    public TimeSeriesCollectionsSettings setHourlyFlushInterval(long hourlyFlushInterval) {
        this.hourlyFlushInterval = hourlyFlushInterval;
        return this;
    }

    public long getDailyFlushInterval() {
        return dailyFlushInterval;
    }

    public TimeSeriesCollectionsSettings setDailyFlushInterval(long dailyFlushInterval) {
        this.dailyFlushInterval = dailyFlushInterval;
        return this;
    }

    public long getWeeklyFlushInterval() {
        return weeklyFlushInterval;
    }

    public TimeSeriesCollectionsSettings setWeeklyFlushInterval(long weeklyFlushInterval) {
        this.weeklyFlushInterval = weeklyFlushInterval;
        return this;
    }

    public long getMainCollectionResolution() {
        return mainCollectionResolution;
    }

    public long getMainCollectionFlushPeriod() {
        return mainCollectionFlushPeriod;
    }
}
