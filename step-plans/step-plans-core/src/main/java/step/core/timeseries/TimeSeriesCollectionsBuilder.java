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

import step.core.collections.CollectionFactory;
import step.core.timeseries.bucket.Bucket;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TimeSeriesCollectionsBuilder {

    public static final String COLLECTION_NAME_SEPARATOR = "_";

    private final CollectionFactory collectionFactory;

    public TimeSeriesCollectionsBuilder(CollectionFactory collectionFactory) {
        this.collectionFactory = collectionFactory;
    }

    public List<TimeSeriesCollection> getTimeSeriesCollections(String mainCollectionName, TimeSeriesCollectionsSettings collectionsSettings, Set<String> ignoredAttributesForHighResolution) {
        List<TimeSeriesCollection> enabledCollections = new ArrayList<>();
        int flushSeriesQueueSize = collectionsSettings.getFlushSeriesQueueSize();
        int flushAsyncQueueSize = collectionsSettings.getFlushAsyncQueueSize();
        //Add additional resolutions
        for (Resolution resolution: Resolution.values()) {
            TimeSeriesCollectionsSettings.ResolutionSettings resolutionSettings = collectionsSettings.getResolutionSettings(resolution);
            if (resolutionSettings != null) {
                addIfEnabled(enabledCollections, mainCollectionName + COLLECTION_NAME_SEPARATOR + resolution.name,
                        resolution.resolution, resolutionSettings.flushInterval, flushSeriesQueueSize, flushAsyncQueueSize,
                        (resolution.coarseResolution ? ignoredAttributesForHighResolution : null), resolutionSettings.enabled);

            }
        }
        return enabledCollections;
    }

    private void addIfEnabled(List<TimeSeriesCollection> enabledCollections, String collectionName, Duration resolution, long flushInterval, int flushSeriesQueueSizeThreshold, int flushAsyncQueueSize, Set<String> ignoredAttributes, boolean enabled) {
        TimeSeriesCollectionSettings settings = new TimeSeriesCollectionSettings()
                .setResolution(resolution.toMillis())
                .setIngestionFlushingPeriodMs(flushInterval)
                .setIngestionFlushSeriesQueueSize(flushSeriesQueueSizeThreshold)
                .setIngestionFlushAsyncQueueSize(flushAsyncQueueSize)
                .setIgnoredAttributes(ignoredAttributes);
        TimeSeriesCollection collection = new TimeSeriesCollection(collectionFactory.getCollection(collectionName, Bucket.class), settings);
        if (enabled) {
            enabledCollections.add(collection);
        } else {
            // disabled resolutions will be completely dropped from db
            collection.drop();
        }
    }
}
