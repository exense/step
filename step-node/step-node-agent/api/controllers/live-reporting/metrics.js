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
 * Metrics channel (Java: step.core.metrics.*): counters/gauges/histograms whose rate-limited samples
 * are batched and POSTed to the controller's /rest/live-reporting/{contextId}/metrics endpoint.
 */

const { logger, LIVEREPORTING_CONTEXT_ID, getReportingUrl, BatchingRestPoster } = require('./shared');

// Instrument type, used as a discriminator in the MetricSample wire format (Java: InstrumentType).
const InstrumentType = Object.freeze({ COUNTER: 'COUNTER', GAUGE: 'GAUGE', HISTOGRAM: 'HISTOGRAM' });

// Minimum interval between observation-triggered flushes (Java: MetricSamplesCollector.FLUSH_INTERVAL_MS).
const METRIC_FLUSH_INTERVAL_MS = 5000;
// Default distribution bucket width for sampled metrics (Java: SampledMetric.DEFAULT_PCL_PRECISION).
const DEFAULT_PERCENTILE_PRECISION = 10;

/**
 * Abstract base for all metric types. Accumulates observations and notifies a framework-installed
 * observation listener (the {@link MetricSamplesCollector}) which decides when to {@link flush}.
 * Single-threaded JS, so plain accumulators replace the Java atomics.
 */
class Metric {
  constructor(name, labels = {}) {
    if (name == null) throw new Error('Metric name cannot be null');
    this.name = name;
    this.labels = labels || {};
    this._observationListener = null;
    this._lastObservedTimestampMs = 0;
  }

  get type() { throw new Error('Metric.type is abstract'); }

  /** Captures and resets the accumulated state into a MetricSample. Reserved for the framework. */
  flush() { throw new Error('Metric.flush is abstract'); }

  /** Reserved for the framework (MetricSamplesCollector). */
  setObservationListener(listener) {
    this._observationListener = listener;
  }

  _notifyObserved(observationTimestampMs) {
    this._lastObservedTimestampMs = Math.max(this._lastObservedTimestampMs, observationTimestampMs);
    if (this._observationListener) this._observationListener(observationTimestampMs);
  }

  _getLastObservedTimestampMs() {
    return this._lastObservedTimestampMs !== 0 ? this._lastObservedTimestampMs : Date.now();
  }

  _sample({ count, sum, min, max, last, distribution }) {
    return {
      sampleTime: this._getLastObservedTimestampMs(),
      name: this.name,
      labels: this.labels,
      type: this.type,
      count,
      sum,
      min,
      max,
      last,
      distribution: distribution != null ? distribution : null,
    };
  }
}

/**
 * Monotonically increasing counter (Java: CounterMetric). Per flush: count = number of increment
 * calls; sum = amount accumulated this interval; min = running total before this interval;
 * max = last = running total. The interval accumulators reset on flush; the total never resets.
 */
class CounterMetric extends Metric {
  constructor(name, labels) {
    super(name, labels);
    this._count = 0;
    this._diff = 0;
    this._total = 0;
  }

  get type() { return InstrumentType.COUNTER; }

  increment(amount = 1, observationTimestampMs = Date.now()) {
    if (amount < 0) throw new Error('Counter increment amount must be non-negative');
    this._count += 1;
    this._diff += amount;
    this._total += amount;
    this._notifyObserved(observationTimestampMs);
    return this;
  }

  flush() {
    const count = this._count;
    const diff = this._diff;
    const total = this._total;
    this._count = 0;
    this._diff = 0;
    return this._sample({ count, sum: diff, min: total - diff, max: total, last: total, distribution: null });
  }
}

/**
 * Base for sampled metrics (gauge, histogram) that track a distribution per interval (Java:
 * SampledMetric). count/sum/min/max/distribution reset on flush; last is retained across flushes.
 */
class SampledMetric extends Metric {
  constructor(name, labels, percentilePrecision = DEFAULT_PERCENTILE_PRECISION) {
    super(name, labels);
    if (percentilePrecision <= 0) throw new Error('percentilePrecision must be positive');
    this._percentilePrecision = percentilePrecision;
    this._count = 0;
    this._sum = 0;
    this._min = Infinity;
    this._max = -Infinity;
    this._distribution = new Map();
    this._last = 0;
  }

  observe(value, observationTimestampMs = Date.now()) {
    this._count += 1;
    this._sum += value;
    if (value < this._min) this._min = value;
    if (value > this._max) this._max = value;
    // Floor to the bucket boundary via division rather than modulo, which avoids floating-point
    // artifacts (e.g. 0.3 % 0.1) if a non-integer value is ever observed.
    const bucket = Math.floor(value / this._percentilePrecision) * this._percentilePrecision;
    this._distribution.set(bucket, (this._distribution.get(bucket) || 0) + 1);
    this._last = value;
    this._notifyObserved(observationTimestampMs);
    return this;
  }

  flush() {
    const count = this._count;
    const sum = this._sum;
    const min = count > 0 ? this._min : 0;
    const max = count > 0 ? this._max : 0;
    const distribution = {};
    for (const [bucket, n] of this._distribution) {
      if (n > 0) distribution[bucket] = n;
    }
    this._count = 0;
    this._sum = 0;
    this._min = Infinity;
    this._max = -Infinity;
    this._distribution = new Map();
    return this._sample({ count, sum, min, max, last: this._last, distribution });
  }
}

class GaugeMetric extends SampledMetric {
  get type() { return InstrumentType.GAUGE; }
}

class HistogramMetric extends SampledMetric {
  get type() { return InstrumentType.HISTOGRAM; }
}

/**
 * Collects MetricSamples from registered metrics (Java: MetricSamplesCollector). On register, an
 * observation listener is installed that rate-limits flushing: the first observation just starts the
 * interval clock; later observations flush only if at least flushIntervalMs has elapsed. {@link close}
 * performs a guaranteed final flush so accumulated values are never lost at keyword end.
 */
class MetricSamplesCollector {
  constructor(forwardConsumer = null, flushIntervalMs = METRIC_FLUSH_INTERVAL_MS) {
    this._forward = forwardConsumer;
    this._flushIntervalMs = flushIntervalMs;
    this._registered = [];
    this._collected = [];
  }

  register(metric) {
    this._registered.push(metric);
    let lastFlushTime = 0;
    metric.setObservationListener((observationTimestampMs) => {
      if (lastFlushTime === 0) {
        // First observation: start the interval clock without flushing; the final flush captures these.
        lastFlushTime = observationTimestampMs;
      } else if (observationTimestampMs - lastFlushTime >= this._flushIntervalMs) {
        lastFlushTime = observationTimestampMs;
        this._collectAndForward(metric.flush());
      }
    });
  }

  /** Final flush of all registered metrics (ignoring the rate limit); returns accumulated samples. */
  getSamples() {
    for (const metric of this._registered) {
      const sample = metric.flush();
      if (sample.count > 0) this._collectAndForward(sample);
    }
    return [...this._collected];
  }

  close() {
    this.getSamples();
  }

  _collectAndForward(sample) {
    // In streaming mode the forward consumer delivers samples; only accumulate in batch mode to avoid
    // unbounded growth over long keyword executions.
    if (this._forward) this._forward(sample);
    else this._collected.push(sample);
  }
}

/**
 * Metric destination that does nothing (no controller context available).
 */
class DiscardingLiveMetricDestination {
  accept(_metric) {}
  async close() {}
}

/**
 * Registers metrics and streams their samples to the controller's live-reporting metrics endpoint.
 * A per-observation collector produces rate-limited samples which are batch-POSTed.
 */
class RestUploadingLiveMetricDestination {
  constructor(endpointUrl, options = {}) {
    this.endpointUrl = endpointUrl;
    this.poster = new BatchingRestPoster(endpointUrl, { label: 'metric', ...options });
    this.collector = new MetricSamplesCollector((sample) => this.poster.add(sample));
  }

  accept(metric) {
    this.collector.register(metric);
  }

  async close() {
    // Final flush forwards any remaining samples to the poster, then drain the poster.
    this.collector.close();
    await this.poster.close();
  }
}

/**
 * Records and streams live metrics (Java: step.reporting.LiveMetrics). Metrics must be registered to
 * be handled by live reporting.
 */
class LiveMetrics {
  constructor(destination) {
    this.destination = destination;
  }

  registerCounter(name, labels) {
    const metric = new CounterMetric(name, labels);
    this.destination.accept(metric);
    return metric;
  }

  registerGauge(name, labels) {
    const metric = new GaugeMetric(name, labels);
    this.destination.accept(metric);
    return metric;
  }

  registerHistogram(name, labels) {
    const metric = new HistogramMetric(name, labels);
    this.destination.accept(metric);
    return metric;
  }

  register(metric) {
    if (metric == null) throw new Error('Metric must not be null');
    this.destination.accept(metric);
    return metric;
  }

  async close() {
    await this.destination.close();
  }
}

/**
 * Builds the live metric destination from the keyword properties, or a discarding one when no
 * controller context is present (local execution). Uses the same context id as measures.
 */
function createMetricDestination(properties) {
  const contextId = properties[LIVEREPORTING_CONTEXT_ID];
  if (!contextId) {
    return new DiscardingLiveMetricDestination();
  }
  let baseUrl;
  try {
    baseUrl = getReportingUrl(properties);
  } catch (e) {
    logger.error('Could not resolve the live-reporting URL, metrics will be discarded:', e);
    return new DiscardingLiveMetricDestination();
  }
  if (!baseUrl) {
    return new DiscardingLiveMetricDestination();
  }
  const metricsUrl = `${baseUrl}/rest/live-reporting/${contextId}/metrics`;
  logger.debug(`Live reporting metrics enabled, endpoint: ${metricsUrl}`);
  return new RestUploadingLiveMetricDestination(metricsUrl);
}

module.exports = {
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
};
