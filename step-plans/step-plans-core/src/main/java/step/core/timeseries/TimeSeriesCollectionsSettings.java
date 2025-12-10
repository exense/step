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

import java.util.Map;
import java.util.TreeMap;

public class TimeSeriesCollectionsSettings {

    public static final String TIME_SERIES_COLLECTION_FLUSH_ASYNC_QUEUE_SIZE = "{collectionName}.flush.async.queue.size";
    public static final String TIME_SERIES_COLLECTION_FLUSH_SERIES_QUEUE_SIZE = "{collectionName}.flush.series.queue.size";
    public static final String RESOLUTION_PROPERTY_PREFIX = "{collectionName}.collections.";
    public static final String TIME_SERIES_RESOLUTION_ENABLED_SUFFIX = ".enabled";
    public static final String TIME_SERIES_RESOLUTION_FLUSH_PERIOD_SUFFIX = ".flush.period";

    //Define the max queue size for series, if the usage is over the limit flush is performed even for partial time interval
    private int flushSeriesQueueSize;
    //flushing do not write to DB directly but to a linked blocking queue in memory which is processed by an asynchronous processor, the queue size is limited to prevent excessive memory usage
    //While the queue is full, ingesting new buckets is blocked
    private int flushAsyncQueueSize;
    //Define the Map of supported Resolutions associated to their settings read from the configuration.
    private final Map<Resolution, ResolutionSettings> resolutionSettings = new TreeMap<>();

    public static class ResolutionSettings {
        //Define the interval of the flushing job for the ingestion pipeline
        //Note that flush is only actually performed by the job if the bucket time interval is complete (i.e. full resolution interval) or
        //if the max series queue size is reached (to limit and control memory usage)
        public final long flushInterval;
        //Flag to completely disable or enable the collection
        public final boolean enabled;

        public ResolutionSettings(long flushInterval, boolean enabled) {
            this.flushInterval = flushInterval;
            this.enabled = enabled;
        }
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

    private void addResolutionSettings(Resolution resolution, ResolutionSettings resolutionSettings) {
        this.resolutionSettings.put(resolution, resolutionSettings);
    }
    public ResolutionSettings getResolutionSettings(Resolution resolution) {
        return resolutionSettings.get(resolution);
    }

    public static TimeSeriesCollectionsSettings readSettings(Configuration configuration, String collectionName) {
        TimeSeriesCollectionsSettings settings = new TimeSeriesCollectionsSettings();
        settings.setFlushSeriesQueueSize(getPropertyAsInteger(configuration, TIME_SERIES_COLLECTION_FLUSH_SERIES_QUEUE_SIZE, collectionName, 20000));
        settings.setFlushAsyncQueueSize(getPropertyAsInteger(configuration, TIME_SERIES_COLLECTION_FLUSH_ASYNC_QUEUE_SIZE, collectionName, 5000));
        //Read settings for additional resolutions
        for (Resolution resolution: Resolution.values()) {
            boolean resolutionEnabled = getPropertyAsBoolean(configuration, RESOLUTION_PROPERTY_PREFIX + resolution.name + TIME_SERIES_RESOLUTION_ENABLED_SUFFIX, collectionName, true);
            Long resolutionFlushInterval = getPropertyAsLong(configuration, RESOLUTION_PROPERTY_PREFIX + resolution.name + TIME_SERIES_RESOLUTION_FLUSH_PERIOD_SUFFIX, collectionName, resolution.defaultFlushPeriod.toMillis());
            settings.addResolutionSettings(resolution, new ResolutionSettings(resolutionFlushInterval, resolutionEnabled));
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
}
