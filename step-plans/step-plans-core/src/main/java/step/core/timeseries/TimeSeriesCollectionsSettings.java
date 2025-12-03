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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class TimeSeriesCollectionsSettings {

    public static final String TIME_SERIES_MAIN_COLLECTION_FLUSH_PERIOD = "{collectionName}.flush.period";
    public static final String TIME_SERIES_COLLECTION_FLUSH_ASYNC_QUEUE_SIZE = "{collectionName}.flush.async.queue.size";
    public static final String TIME_SERIES_COLLECTION_FLUSH_SERIES_QUEUE_SIZE = "{collectionName}.flush.series.queue.size";
    public static final String TIME_SERIES_MAIN_RESOLUTION = "{collectionName}.resolution";
    public static final String RESOLUTION_PROPERTY_PREFIX = "{collectionName}.collections.";
    public static final String TIME_SERIES_RESOLUTION_ENABLED_SUFFIX = ".enabled";
    public static final String TIME_SERIES_RESOLUTION_FLUSH_PERIOD_SUFFIX = ".flush.period";

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
    private final Map<TimeSeriesCollectionsBuilder.Resolution, ResolutionSettings> additionalResolutionSettings = new TreeMap<>();

    public static class ResolutionSettings {
        public final long flushInterval;
        public final boolean enabled;

        public ResolutionSettings(long flushInterval, boolean enabled) {
            this.flushInterval = flushInterval;
            this.enabled = enabled;
        }
    }

    public long getMainResolution() {
        return mainResolution;
    }

    public void setMainResolution(long mainResolution) {
        this.mainResolution = mainResolution;
    }

    public long getMainFlushInterval() {
        return mainFlushInterval;
    }

    public void setMainFlushInterval(long mainFlushInterval) {
        this.mainFlushInterval = mainFlushInterval;
    }

    public int getFlushSeriesQueueSize() {
        return flushSeriesQueueSize;
    }

    public void setFlushSeriesQueueSize(int flushSeriesQueueSize) {
        this.flushSeriesQueueSize = flushSeriesQueueSize;
    }

    private void setFlushAsyncQueueSize(int flushAsyncQueueSize) {
        this.flushAsyncQueueSize = flushAsyncQueueSize;
    }

    public int getFlushAsyncQueueSize() {
        return flushAsyncQueueSize;
    }

    private void addAdditionalResolutionSettings(TimeSeriesCollectionsBuilder.Resolution resolution, ResolutionSettings resolutionSettings) {
        additionalResolutionSettings.put(resolution, resolutionSettings);
    }
    public ResolutionSettings getResolutionSettings(TimeSeriesCollectionsBuilder.Resolution resolution) {
        return additionalResolutionSettings.get(resolution);
    }

    public static TimeSeriesCollectionsSettings readSettings(Configuration configuration, String collectionName) {
        // Validate and read main resolution settings
        long mainResolution = getPropertyAsLong(configuration, TIME_SERIES_MAIN_RESOLUTION, collectionName, 5000L);
        validateMainResolutionParam(mainResolution);
        TimeSeriesCollectionsSettings settings = new TimeSeriesCollectionsSettings();
        Long mainResolutionFlushInterval = getPropertyAsLong(configuration, TIME_SERIES_MAIN_COLLECTION_FLUSH_PERIOD, collectionName, Duration.ofSeconds(1).toMillis());
        settings.setMainResolution(mainResolution);
        settings.setMainFlushInterval(mainResolutionFlushInterval);
        settings.setFlushSeriesQueueSize(getPropertyAsInteger(configuration, TIME_SERIES_COLLECTION_FLUSH_SERIES_QUEUE_SIZE, collectionName, 20000));
        settings.setFlushAsyncQueueSize(getPropertyAsInteger(configuration, TIME_SERIES_COLLECTION_FLUSH_ASYNC_QUEUE_SIZE, collectionName, 5000));
        //Read settings for additional resolutions
        for (TimeSeriesCollectionsBuilder.Resolution resolution: TimeSeriesCollectionsBuilder.Resolution.values()){
            boolean resolutionEnabled = getPropertyAsBoolean(configuration, RESOLUTION_PROPERTY_PREFIX + resolution.name + TIME_SERIES_RESOLUTION_ENABLED_SUFFIX, collectionName, true);
            Long resolutionFlushInterval = getPropertyAsLong(configuration, RESOLUTION_PROPERTY_PREFIX + resolution.name + TIME_SERIES_RESOLUTION_FLUSH_PERIOD_SUFFIX, collectionName, resolution.defaultFlushPeriod.toMillis());
            settings.addAdditionalResolutionSettings(resolution, new ResolutionSettings(resolutionFlushInterval, resolutionEnabled));
        }
        return settings;
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
        timeSeriesCollectionsSettings.setMainResolution(mainResolution);
        timeSeriesCollectionsSettings.setMainFlushInterval(mainFlushInterval);
        return timeSeriesCollectionsSettings;
    }

}
