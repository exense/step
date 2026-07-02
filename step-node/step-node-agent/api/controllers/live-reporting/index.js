/*
 * Copyright (C) 2026, exense GmbH
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

/**
 * Live reporting for the Node.js agent.
 *
 * Mirrors the Java `step.reporting.LiveReporting` façade: it lets a running keyword push data
 * to the controller *during* execution, rather than only in the final Output. Built per keyword
 * call via `createLiveReporting(properties)` and exposed to keywords as the `liveReporting` argument.
 *
 * Three channels (each enabled by context properties the controller passes in the keyword Input;
 * when they are absent — e.g. local runner execution — a discarding no-op is installed, so keyword
 * code can always call the API safely). Each channel lives in its own module:
 *
 *  - `measures`     — ./measures      live performance measures, batched and POSTed
 *  - `metrics`      — ./metrics       counters/gauges/histograms; rate-limited samples batched and POSTed
 *  - `fileUploads`  — ./file-uploads  streaming file/resource uploads over WebSocket (incl. growing files)
 *
 * Shared infrastructure (logger, reporting-URL resolution, the batching REST poster) lives in ./shared.
 */

const {
  DiscardingLiveMeasureDestination,
  RestUploadingLiveMeasureDestination,
  LiveMeasures,
  createMeasureDestination,
} = require('./measures');
const {
  InstrumentType,
  Metric,
  CounterMetric,
  SampledMetric,
  GaugeMetric,
  HistogramMetric,
  MetricSamplesCollector,
  DiscardingLiveMetricDestination,
  RestUploadingLiveMetricDestination,
  LiveMetrics,
  createMetricDestination,
} = require('./metrics');
const {
  QuotaExceededError,
  StreamingUploads,
  StreamingUpload,
  discardingUploader,
  createFileUploads,
} = require('./file-uploads');

/**
 * Container for the live-reporting channels exposed to keywords.
 * Mirrors Java `step.reporting.LiveReporting`: measures (REST), metrics (REST), file uploads (WebSocket).
 */
class LiveReporting {
  constructor({ measureDestination, metricDestination, fileUploads } = {}) {
    this.measures = new LiveMeasures(measureDestination || new DiscardingLiveMeasureDestination());
    this.metrics = new LiveMetrics(metricDestination || new DiscardingLiveMetricDestination());
    this.fileUploads = fileUploads || new StreamingUploads(discardingUploader);
  }

  async close() {
    await Promise.allSettled([this.measures.close(), this.metrics.close(), this.fileUploads.close()]);
  }
}

/**
 * Creates the LiveReporting object for a single keyword execution.
 * @param {object} [properties] the merged keyword properties (agent + token + input)
 * @returns {LiveReporting}
 */
function createLiveReporting(properties = {}) {
  return new LiveReporting({
    measureDestination: createMeasureDestination(properties),
    metricDestination: createMetricDestination(properties),
    fileUploads: createFileUploads(properties),
  });
}

module.exports = {
  createLiveReporting,
  LiveReporting,
  // measures
  LiveMeasures,
  RestUploadingLiveMeasureDestination,
  DiscardingLiveMeasureDestination,
  // metrics
  LiveMetrics,
  RestUploadingLiveMetricDestination,
  DiscardingLiveMetricDestination,
  MetricSamplesCollector,
  Metric,
  CounterMetric,
  SampledMetric,
  GaugeMetric,
  HistogramMetric,
  InstrumentType,
  // file uploads
  StreamingUploads,
  StreamingUpload,
  QuotaExceededError,
};
