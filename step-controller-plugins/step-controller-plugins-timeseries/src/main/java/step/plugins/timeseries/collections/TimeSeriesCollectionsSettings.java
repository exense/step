package step.plugins.timeseries.collections;

public class TimeSeriesCollectionsSettings {
    private boolean perMinute;
    private boolean hourly;
    private boolean daily;
    private boolean weekly;
    private boolean monthly;

    public boolean isDailyEnabled() {
        return daily;
    }

    public boolean isPerMinuteEnabled() {
        return perMinute;
    }

    public boolean isHourlyEnabled() {
        return hourly;
    }

    public boolean isWeeklyEnabled() {
        return weekly;
    }

    public boolean isMonthlyEnabled() {
        return monthly;
    }

    public TimeSeriesCollectionsSettings setPerMinute(boolean perMinute) {
        this.perMinute = perMinute;
        return this;
    }

    public TimeSeriesCollectionsSettings setHourly(boolean hourly) {
        this.hourly = hourly;
        return this;
    }

    public TimeSeriesCollectionsSettings setDaily(boolean daily) {
        this.daily = daily;
        return this;
    }

    public TimeSeriesCollectionsSettings setWeekly(boolean weekly) {
        this.weekly = weekly;
        return this;
    }

    public TimeSeriesCollectionsSettings setMonthly(boolean monthly) {
        this.monthly = monthly;
        return this;
    }
}
