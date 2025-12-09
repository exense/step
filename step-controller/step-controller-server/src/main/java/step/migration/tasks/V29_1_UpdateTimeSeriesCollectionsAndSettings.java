/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.migration.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.timeseries.Resolution;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static step.core.controller.ControllerSettingPlugin.SETTINGS;

public class V29_1_UpdateTimeSeriesCollectionsAndSettings extends MigrationTask {

    public static final String TIME_SERIES_MAIN_COLLECTION = "timeseries";
    public static final String TIME_SERIES_MAIN_COLLECTION_NEW_NAME = "timeseries_5_seconds";
    public static final String TIME_SERIES_MAIN_COLLECTION_REPORTS = "reportNodeTimeSeries";
    public static final String TIME_SERIES_MAIN_COLLECTION_REPORTS_NEW_NAME = "reportNodeTimeSeries_5_seconds";
    public static final String HOUSEKEEPING_TIME_SERIES_DEFAULT_TTL = "housekeeping_time_series_default_ttl";
    public static final String HOUSEKEEPING_TIME_SERIES_PER_MINUTE_TTL = "housekeeping_time_series_per_minute_ttl";
    public static final String HOUSEKEEPING_TIME_SERIES_HOURLY_TTL = "housekeeping_time_series_hourly_ttl";
    public static final String HOUSEKEEPING_TIME_SERIES_DAILY_TTL = "housekeeping_time_series_daily_ttl";
    public static final String HOUSEKEEPING_TIME_SERIES_WEEKLY_TTL = "housekeeping_time_series_weekly_ttl";
    public static final String HOUSEKEEPING_TIME_SERIES_TTL_PREFIX = "housekeeping_time_series_";
    public static final String HOUSEKEEPING_TIME_SERIES_TTL_SUFFIX = "_ttl";

    private static final Logger log = LoggerFactory.getLogger(V29_1_UpdateTimeSeriesCollectionsAndSettings.class);
    private final Collection<Document> timeseriesCollection;
    private final Collection<Document> settings;
    private final Collection<Document> reportNodeTimeseriesCollection;
    protected AtomicInteger successCount;

    public V29_1_UpdateTimeSeriesCollectionsAndSettings(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3,29,1), collectionFactory, migrationContext);
        timeseriesCollection = collectionFactory.getCollection(TIME_SERIES_MAIN_COLLECTION, Document.class);
        reportNodeTimeseriesCollection = collectionFactory.getCollection(TIME_SERIES_MAIN_COLLECTION_REPORTS, Document.class);
        settings = collectionFactory.getCollection(SETTINGS, Document.class);
    }

    @Override
    public void runUpgradeScript() {
        log.info("Renaming the 'main' collection of the response times time-series to include its resolution");
        timeseriesCollection.rename(TIME_SERIES_MAIN_COLLECTION_NEW_NAME);

        log.info("Renaming the 'main' collection of the report nodes time-series to include its resolution");
        reportNodeTimeseriesCollection.rename(TIME_SERIES_MAIN_COLLECTION_REPORTS_NEW_NAME);


        log.info("Renaming time-series housekeeping setting keys");
        //use names from enum which will then be aligned with the collection names
        updateSettingKeyIfPresent(HOUSEKEEPING_TIME_SERIES_DEFAULT_TTL, HOUSEKEEPING_TIME_SERIES_TTL_PREFIX + Resolution.FIVE_SECONDS.name + HOUSEKEEPING_TIME_SERIES_TTL_SUFFIX);
        updateSettingKeyIfPresent(HOUSEKEEPING_TIME_SERIES_PER_MINUTE_TTL, HOUSEKEEPING_TIME_SERIES_TTL_PREFIX + Resolution.ONE_MINUTE.name + HOUSEKEEPING_TIME_SERIES_TTL_SUFFIX);
        updateSettingKeyIfPresent(HOUSEKEEPING_TIME_SERIES_HOURLY_TTL, HOUSEKEEPING_TIME_SERIES_TTL_PREFIX + Resolution.ONE_HOUR.name + HOUSEKEEPING_TIME_SERIES_TTL_SUFFIX);
        updateSettingKeyIfPresent(HOUSEKEEPING_TIME_SERIES_DAILY_TTL, HOUSEKEEPING_TIME_SERIES_TTL_PREFIX + Resolution.ONE_DAY.name + HOUSEKEEPING_TIME_SERIES_TTL_SUFFIX);
        updateSettingKeyIfPresent(HOUSEKEEPING_TIME_SERIES_WEEKLY_TTL, HOUSEKEEPING_TIME_SERIES_TTL_PREFIX + Resolution.ONE_WEEK.name + HOUSEKEEPING_TIME_SERIES_TTL_SUFFIX);
        log.info("Time-series housekeeping setting keys renamed");
    }

    private void updateSettingKeyIfPresent(String oldKey, String newKey) {
        Optional<Document> setting = settings.find(Filters.equals("key", oldKey), null, null, null, 0).findFirst();
        setting.ifPresent(s -> {
            s.put("key", newKey);
            settings.save(s);
            logger.info("Time-series housekeeping setting key {} renamed to {}", oldKey, newKey);
        });

    }

    @Override
    public void runDowngradeScript() {

    }
}
