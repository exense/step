/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.core.timeseries;

import ch.exense.commons.app.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class TimeSeriesCollectionsSettings {

    public static final String TIME_SERIES_MAIN_COLLECTION_FLUSH_PERIOD = "{collectionName}.flush.period";
    public static final String TIME_SERIES_COLLECTION_FLUSH_ASYNC_QUEUE_SIZE = "{collectionName}.flush.async.queue.size";
    public static final String TIME_SERIES_COLLECTION_FLUSH_SERIES_QUEUE_SIZE = "{collectionName}.flush.series.queue.size";
    public static final String TIME_SERIES_MAIN_RESOLUTION = "{collectionName}.resolution";
    public static final String TIME_SERIES_MINUTE_COLLECTION_ENABLED = "{collectionName}.collections.minute.enabled";
    public static final String TIME_SERIES_MINUTE_COLLECTION_FLUSH_PERIOD = "{collectionName}.collections.minute.flush.period";
    public static final String TIME_SERIES_HOUR_COLLECTION_ENABLED = "{collectionName}.collections.hour.enabled";
    public static final String TIME_SERIES_HOUR_COLLECTION_FLUSH_PERIOD = "{collectionName}.collections.hour.flush.period";
    public static final String TIME_SERIES_DAY_COLLECTION_ENABLED = "{collectionName}.collections.day.enabled";
    public static final String TIME_SERIES_DAY_COLLECTION_FLUSH_PERIOD = "{collectionName}.collections.day.flush.period";
    public static final String TIME_SERIES_WEEK_COLLECTION_ENABLED = "{collectionName}.collections.week.enabled";
    public static final String TIME_SERIES_WEEK_COLLECTION_FLUSH_PERIOD = "{collectionName}.collections.week.flush.period";

    private long mainResolution;
    //Define the interval of the flushing job for the main ingestion pipeline (highest resolution)
    //Note that flush is only actually performed by the job if the bucket time interval is complete (i.e. full resolution interval) or
    //if the max series queue size is reached (to limit and control memory usage)
    private long mainFlushInterval;
    //Define the max queue size for series, if the usage is over the limit flush is performed even for partial time interval
    private int flushSeriesQueueSize;
    //flushing do not write to DB directly but to a linked blocking queue in memory which is processed by an asynchronous processor, the queue size is limited to prevent excessive memory usage
    //While the queue is full, ingesting new buckets is blocked
    private int flushAsyncQueueSize;
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

    public long getMainResolution() {
        return mainResolution;
    }

    public TimeSeriesCollectionsSettings setMainResolution(long mainResolution) {
        this.mainResolution = mainResolution;
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

    public long getMainFlushInterval() {
        return mainFlushInterval;
    }

    public TimeSeriesCollectionsSettings setMainFlushInterval(long mainFlushInterval) {
        this.mainFlushInterval = mainFlushInterval;
        return this;
    }

    public long getPerMinuteFlushInterval() {
        return perMinuteFlushInterval;
    }

    public int getFlushSeriesQueueSize() {
        return flushSeriesQueueSize;
    }

    public TimeSeriesCollectionsSettings setFlushSeriesQueueSize(int flushSeriesQueueSize) {
        this.flushSeriesQueueSize = flushSeriesQueueSize;
        return this;
    }

    private TimeSeriesCollectionsSettings setFlushAsyncQueueSize(int flushAsyncQueueSize) {
        this.flushAsyncQueueSize = flushAsyncQueueSize;
        return this;
    }

    public int getFlushAsyncQueueSize() {
        return flushAsyncQueueSize;
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

    public static TimeSeriesCollectionsSettings readSettings(Configuration configuration, String collectionName) {
        long mainResolution = getPropertyAsLong(configuration, TIME_SERIES_MAIN_RESOLUTION, collectionName, 5000L);
        validateMainResolutionParam(mainResolution);
        return new TimeSeriesCollectionsSettings()
                .setMainResolution(mainResolution)
                .setMainFlushInterval(getPropertyAsLong(configuration, TIME_SERIES_MAIN_COLLECTION_FLUSH_PERIOD, collectionName, Duration.ofSeconds(1).toMillis()))
                .setFlushSeriesQueueSize(getPropertyAsInteger(configuration, TIME_SERIES_COLLECTION_FLUSH_SERIES_QUEUE_SIZE, collectionName, 20000))
                .setFlushAsyncQueueSize(getPropertyAsInteger(configuration, TIME_SERIES_COLLECTION_FLUSH_ASYNC_QUEUE_SIZE, collectionName, 5000))
                .setPerMinuteEnabled(getPropertyAsBoolean(configuration, TIME_SERIES_MINUTE_COLLECTION_ENABLED, collectionName, true))
                .setPerMinuteFlushInterval(getPropertyAsLong(configuration, TIME_SERIES_MINUTE_COLLECTION_FLUSH_PERIOD, collectionName, Duration.ofMinutes(1).toMillis()))
                .setHourlyEnabled(getPropertyAsBoolean(configuration, TIME_SERIES_HOUR_COLLECTION_ENABLED, collectionName, true))
                .setHourlyFlushInterval(getPropertyAsLong(configuration, TIME_SERIES_HOUR_COLLECTION_FLUSH_PERIOD, collectionName, Duration.ofMinutes(5).toMillis()))
                .setDailyEnabled(getPropertyAsBoolean(configuration, TIME_SERIES_DAY_COLLECTION_ENABLED, collectionName, true))
                .setDailyFlushInterval(getPropertyAsLong(configuration, TIME_SERIES_DAY_COLLECTION_FLUSH_PERIOD, collectionName, Duration.ofHours(1).toMillis()))
                .setWeeklyEnabled(getPropertyAsBoolean(configuration, TIME_SERIES_WEEK_COLLECTION_ENABLED, collectionName, true))
                .setWeeklyFlushInterval(getPropertyAsLong(configuration, TIME_SERIES_WEEK_COLLECTION_FLUSH_PERIOD, collectionName, Duration.ofHours(2).toMillis()));
    }

    private static Long getPropertyAsLong(Configuration configuration, String property, String collectionName, long defaultValue) {
        return configuration.getPropertyAsLong(property(property, collectionName), defaultValue);
    }

    private static int getPropertyAsInteger(Configuration configuration, String property, String collectionName, int defaultValue) {
        return configuration.getPropertyAsInteger(property(property, collectionName), defaultValue);
    }

    private static boolean getPropertyAsBoolean(Configuration configuration, String property, String collectionName, boolean defaultValue) {
        return configuration.getPropertyAsBoolean(property(property, collectionName), defaultValue);
    }

    private static String property(String propertyValue, String collectionName) {
        return propertyValue.replaceAll("\\{collectionName\\}", collectionName);
    }

    private static void validateMainResolutionParam(long resolution) {
        double msInMinute = TimeUnit.MINUTES.toMillis(1);
        if (msInMinute % resolution != 0) {
            throw new IllegalArgumentException("Invalid interval: " + resolution + " seconds. The interval must be a divisor of one minute (60 seconds).");
        }
    }

    public static TimeSeriesCollectionsSettings buildSingleResolutionSettings(long mainResolution, long mainFlushInterval) {
        TimeSeriesCollectionsSettings timeSeriesCollectionsSettings = new TimeSeriesCollectionsSettings();
        timeSeriesCollectionsSettings.setDailyEnabled(false);
        timeSeriesCollectionsSettings.setHourlyEnabled(false);
        timeSeriesCollectionsSettings.setPerMinuteEnabled(false);
        timeSeriesCollectionsSettings.setWeeklyEnabled(false);
        timeSeriesCollectionsSettings.setMainResolution(mainResolution);
        timeSeriesCollectionsSettings.setMainFlushInterval(mainFlushInterval);
        timeSeriesCollectionsSettings.setFlushSeriesQueueSize(20000);
        return timeSeriesCollectionsSettings;
    }

}
