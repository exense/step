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
 * code can always call the API safely):
 *
 *  - `measures`     — live performance measures, batched and POSTed (Java: RestUploadingLiveMeasureDestination)
 *                       POST {controllerUrl}/rest/live-reporting/{contextId}/measures
 *                       Body: [ { name, begin, duration, status, data }, ... ]   // expects HTTP 204
 *  - `metrics`      — counters/gauges/histograms; rate-limited samples batched and POSTed (Java: RestUploadingLiveMetricDestination)
 *                       POST {controllerUrl}/rest/live-reporting/{contextId}/metrics
 *                       Body: [ { sampleTime, name, labels, type, count, sum, min, max, last, distribution }, ... ]   // expects HTTP 204
 *  - `fileUploads`  — streaming file/resource uploads over WebSocket, incl. files still being written
 *                     (Java: step.streaming WebsocketUploadClient)
 *                       ws(s)://{host}/{uploadPath}?streamingUploadContextId={id}
 *                       StartUpload -> ReadyForUpload -> (binary message) -> FinishUpload -> UploadAcknowledged -> close
 */

const http = require('http');
const https = require('https');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

// MeasureStatus and its validation are shared with the (final) OutputBuilder measures, so the
// live and end-of-keyword measure APIs behave identically.
const { MeasureStatus } = require('./output');

// In the forked keyword process this file is copied next to its dependencies (agent-fork-libs),
// where the agent logger is not reachable. Fall back to the console in that case (same as session.js).
let logger;
try {
  logger = require('../logger').child({ component: 'LiveReporting' });
} catch {
  logger = { debug: console.debug.bind(console), info: console.log.bind(console), warn: console.warn.bind(console), error: console.error.bind(console) };
}

// Property keys injected by the controller (Java: LiveReportingConstants / StreamingResourceUploadContext).
const LIVEREPORTING_CONTEXT_ID = '$liveReporting.contextId';
const LIVEREPORTING_CONTROLLER_URL = '$liveReporting.controllerUrl';
const STREAMING_WEBSOCKET_UPLOAD_PATH = '$liveReporting.streaming.websocket.upload.path';
const STREAMING_UPLOAD_CONTEXT_ID = 'streamingUploadContextId';
// Agent-side override of the reporting base URL (Java: getReportingUrl).
const REPORTING_URL_AGENT_OVERRIDE = 'step.reporting.url';
// Env var through which the parent agent passes the absolute path of its bundled `ws` module to the
// forked keyword process (whose own require() resolves against the keyword project, not the agent).
const WS_MODULE_ENV = 'STEP_AGENT_WS_MODULE';

const DEFAULT_BATCH_SIZE = 500;
const DEFAULT_FLUSH_INTERVAL_MS = 5000;
const DEFAULT_READ_TIMEOUT_MS = 30_000;
// Generous upper bound for a single file upload handshake (Java client uses 60s for each step).
const DEFAULT_UPLOAD_TIMEOUT_MS = 60_000;
// Interval between polls of a growing file for newly appended bytes (Java: DEFAULT_FILE_POLL_INTERVAL_MS).
const DEFAULT_FILE_POLL_INTERVAL_MS = 100;
// Max bytes read (and sent as one fragment) per tail iteration, to bound memory regardless of file size.
const MAX_READ_CHUNK_BYTES = 64 * 1024;
// WebSocket close codes / reason phrases (Java: UploadProtocolMessage / CloseReasonUtil).
const WS_NORMAL_CLOSURE = 1000;
const WS_UNEXPECTED_CONDITION = 1011;
const CLOSEREASON_UPLOAD_COMPLETED = 'Upload completed';
const QUOTA_EXCEEDED_PREFIX = 'QuotaExceededException: ';

const VALID_STATUSES = new Set(Object.values(MeasureStatus));

function assertValidStatus(status) {
  if (status !== undefined && !VALID_STATUSES.has(status)) {
    throw new TypeError(`Invalid measure status: "${status}". Must be one of: ${[...VALID_STATUSES].join(', ')}`);
  }
}

/**
 * Resolves the reporting base URL from the (already merged) keyword properties.
 * The agent-side override takes precedence over the controller-provided default, matching the Java client.
 * @returns {string|null} base URL without trailing slash, or null when not running against a controller.
 */
function getReportingUrl(properties) {
  let url;
  const agentConfUrl = properties[REPORTING_URL_AGENT_OVERRIDE];
  if (agentConfUrl != null) {
    if (!/^https?:\/\/.+/.test(agentConfUrl)) {
      throw new Error(`Invalid URL in '${REPORTING_URL_AGENT_OVERRIDE}' (agent-side configuration): ${agentConfUrl}`);
    }
    url = agentConfUrl;
  } else {
    url = properties[LIVEREPORTING_CONTROLLER_URL];
  }
  while (url != null && url.endsWith('/')) {
    url = url.substring(0, url.length - 1);
  }
  return url ?? null;
}

/**
 * Measure destination that does nothing. Installed when no controller context is available
 * (e.g. local runner execution), so keyword code can always call the measures API.
 */
class DiscardingLiveMeasureDestination {
  accept(_measure) {}
  async close() {}
}

/**
 * Buffers items and POSTs them as JSON array batches to a controller endpoint. A batch is sent when
 * it reaches {@link batchSize} or when the flush interval elapses. {@link _send} never rejects
 * (errors are logged), so fire-and-forget flushes can't surface as unhandled rejections — which the
 * keyword fork would otherwise report as a keyword error. Shared by the measures and metrics channels.
 */
class BatchingRestPoster {
  constructor(endpointUrl, { label = 'item', batchSize = DEFAULT_BATCH_SIZE, flushIntervalMs = DEFAULT_FLUSH_INTERVAL_MS } = {}) {
    this.endpointUrl = endpointUrl;
    this.label = label;
    this.batchSize = batchSize;
    this.buffer = [];
    this.closed = false;
    this.inFlight = new Set();
    // Periodic flush. unref() so the timer never keeps the (forked) process alive on its own.
    this.timer = setInterval(() => this.flush(), flushIntervalMs);
    if (typeof this.timer.unref === 'function') this.timer.unref();
  }

  add(item) {
    if (this.closed) {
      logger.warn(`${this.label} received after live reporting was closed; discarding`);
      return;
    }
    this.buffer.push(item);
    if (this.buffer.length >= this.batchSize) {
      this.flush();
    }
  }

  flush() {
    if (this.buffer.length === 0) return;
    const batch = this.buffer;
    this.buffer = [];
    const promise = this._send(batch).finally(() => this.inFlight.delete(promise));
    this.inFlight.add(promise);
  }

  _send(items) {
    return new Promise((resolve) => {
      let parsedUrl;
      try {
        parsedUrl = new URL(this.endpointUrl);
      } catch (e) {
        logger.error(`Invalid live-reporting URL '${this.endpointUrl}':`, e);
        return resolve();
      }
      const httpModule = parsedUrl.protocol === 'https:' ? https : http;
      const body = Buffer.from(JSON.stringify(items), 'utf8');
      const req = httpModule.request(
        {
          hostname: parsedUrl.hostname,
          port: parsedUrl.port,
          path: parsedUrl.pathname + parsedUrl.search,
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Content-Length': body.length,
          },
        },
        (resp) => {
          const status = resp.statusCode;
          // The response stream can emit 'error' (e.g. connection severed mid-response); handle it so
          // it doesn't become an unhandled exception, and settle the flush promise either way.
          resp.on('error', (err) => {
            logger.error(`Response error while reporting ${items.length} ${this.label}(s):`, err);
            resolve();
          });
          // Always drain the response so the socket can be reused/released.
          resp.on('data', () => {});
          resp.on('end', () => {
            if (status !== 204) {
              logger.error(`Error while reporting ${items.length} ${this.label}(s). The live reporting service returned ${status}`);
            }
            resolve();
          });
        }
      );
      req.setTimeout(DEFAULT_READ_TIMEOUT_MS, () => {
        req.destroy(new Error(`Live reporting request timed out after ${DEFAULT_READ_TIMEOUT_MS}ms`));
      });
      req.on('error', (err) => {
        logger.error(`Error while reporting ${items.length} ${this.label}(s) to ${this.endpointUrl}:`, err);
        resolve();
      });
      req.end(body);
    });
  }

  async close() {
    this.closed = true;
    clearInterval(this.timer);
    // Flush whatever is buffered, then wait for all outstanding requests to settle.
    this.flush();
    await Promise.allSettled([...this.inFlight]);
  }
}

/**
 * Buffers measures and POSTs them as JSON batches to the controller's live-reporting endpoint.
 */
class RestUploadingLiveMeasureDestination {
  constructor(endpointUrl, options = {}) {
    this.endpointUrl = endpointUrl;
    this.poster = new BatchingRestPoster(endpointUrl, { label: 'measure', ...options });
  }

  accept(measure) {
    this.poster.add(measure);
  }

  async close() {
    await this.poster.close();
  }
}

/**
 * Records and streams live performance (response-time) measures.
 * Supports a stack of nested measures via startMeasure()/stopMeasure(), plus direct addMeasure().
 * The API matches the (final) OutputBuilder measure API for consistency.
 *
 * Concurrency: startMeasure()/stopMeasure() share a single stack and are meant for sequential or
 * properly nested measurements within one logical flow. Overlapping measures started from concurrent
 * asynchronous operations (e.g. several branches of a Promise.all) would interleave their push/pop and
 * corrupt the stack — for those cases use addMeasure(), which records a fully-formed measure atomically.
 * (This mirrors the Java LiveMeasures stack semantics.)
 */
class LiveMeasures {
  constructor(destination) {
    this.destination = destination;
    this._stack = [];
  }

  /**
   * Immediately submits a fully-constructed measure.
   * @param {string} name
   * @param {number} durationMillis
   * @param {{ begin?: number, data?: object, status?: string }} [options]
   */
  addMeasure(name, durationMillis, { begin, data, status = MeasureStatus.PASSED } = {}) {
    if (name == null) throw new Error('measure name must not be null');
    assertValidStatus(status);
    const measure = { name, duration: durationMillis, status };
    if (begin !== undefined) measure.begin = begin;
    if (data !== undefined) measure.data = data;
    this.destination.accept(measure);
  }

  /**
   * Starts a measure and pushes it onto the internal stack. Must be matched by stopMeasure().
   * @param {string} name
   * @param {number} [begin=Date.now()]
   */
  startMeasure(name, begin = Date.now()) {
    if (name == null) throw new Error('measureName must not be null');
    this._stack.push({ name, begin });
  }

  /**
   * Stops the most recently started measure, computes its duration, and submits it.
   * @param {{ status?: string, data?: object }} [options]
   */
  stopMeasure({ status = MeasureStatus.PASSED, data } = {}) {
    assertValidStatus(status);
    const current = this._stack.pop();
    if (!current) {
      throw new Error('Unbalanced measures stack: stopMeasure() called but no measure present; did you forget to call startMeasure()?');
    }
    this.addMeasure(current.name, Date.now() - current.begin, { begin: current.begin, status, data });
  }

  async close() {
    if (this._stack.length > 0) {
      logger.warn(`LiveMeasures closing, but there are still ${this._stack.length} ongoing measure(s); these will be discarded`);
      this._stack = [];
    }
    await this.destination.close();
  }
}

// ---------------------------------------------------------------------------
// Metrics channel (Java: step.core.metrics.*)
// ---------------------------------------------------------------------------

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
 * Raised when the controller rejects an upload because of quota restrictions.
 * Mirrors Java `step.streaming.common.QuotaExceededException`.
 */
class QuotaExceededError extends Error {
  constructor(message) {
    super(message);
    this.name = 'QuotaExceededError';
  }
}

// Loads the `ws` module: in the forked keyword process via the absolute path the parent injected,
// otherwise (e.g. unit tests running in the agent process) via normal resolution. Returns null if
// unavailable, in which case file uploads degrade to discarding.
function loadWebSocket() {
  try {
    const injectedPath = process.env[WS_MODULE_ENV];
    return injectedPath ? require(injectedPath) : require('ws');
  } catch (e) {
    logger.debug('Could not load the ws module, file uploads will be disabled:', e && e.message ? e.message : e);
    return null;
  }
}

function delay(ms) {
  return new Promise((resolve) => {
    const t = setTimeout(resolve, ms);
    if (typeof t.unref === 'function') t.unref();
  });
}

/**
 * Drives a single streaming file upload over the WebSocket protocol, mirroring the handshake of
 * Java `WebsocketUploadClient`:
 *   StartUpload -> ReadyForUpload -> (one binary message, fragmented) -> FinishUpload -> UploadAcknowledged -> close.
 *
 * The file content is streamed as it grows: a background loop tails the file (Java:
 * LiveFileInputStream), sending appended bytes as non-final fragments of a single binary message
 * and updating a running MD5. Calling {@link complete} signals end-of-input, after which the loop
 * drains any remaining bytes, terminates the message, and sends FinishUpload.
 *
 * Sending the whole file before calling complete() (the convenience uploadBinaryFile/uploadTextFile
 * path) is just the degenerate case where end-of-input is signalled immediately.
 */
class WebsocketUploadController {
  constructor(WebSocket, endpointUri, filePath, metadata) {
    this.filePath = filePath;
    this.metadata = metadata;
    this.md5 = crypto.createHash('md5');
    this.reference = null;
    this.state = 'CONNECTING';
    this.settled = false;
    this.clientChecksum = null;
    // Transport/streaming error remembered as a fallback; the websocket close is the authority for the
    // final status, so that a server quota close reason takes precedence over an incidental send error.
    this._streamError = null;

    // End-of-input signal (Java: EndOfInputSignal). _eofError, when set, aborts the stream loop.
    this._eofDone = false;
    this._eofError = null;
    this._eofResolve = null;
    this._eofPromise = new Promise((resolve) => { this._eofResolve = resolve; });

    this.finalStatus = new Promise((resolve, reject) => {
      this._resolveFinal = resolve;
      this._rejectFinal = reject;
    });
    // Prevent an unhandled rejection if nobody awaits finalStatus (e.g. an upload started and never
    // completed, then cancelled on close). Real awaiters still observe the rejection.
    this.finalStatus.catch(() => {});

    this.socket = new WebSocket(endpointUri);
    this.timeout = setTimeout(
      () => this._fail(new Error(`Upload timed out after ${DEFAULT_UPLOAD_TIMEOUT_MS}ms`)),
      DEFAULT_UPLOAD_TIMEOUT_MS
    );
    if (typeof this.timeout.unref === 'function') this.timeout.unref();

    this.socket.on('open', () => this.socket.send(JSON.stringify({ '@': 'StartUpload', metadata: this.metadata })));
    this.socket.on('message', (data) => this._onMessage(data));
    this.socket.on('close', (code, reasonBuf) => this._onClose(code, reasonBuf));
    this.socket.on('error', (err) => this._abort(err));
  }

  _settleResolve(value) {
    if (!this.settled) {
      this.settled = true;
      clearTimeout(this.timeout);
      this._resolveFinal(value);
    }
  }

  _settleReject(err) {
    if (!this.settled) {
      this.settled = true;
      clearTimeout(this.timeout);
      this._rejectFinal(err);
    }
  }

  _fail(err) {
    this._settleReject(err);
    try { this.socket.terminate(); } catch { /* already closing */ }
  }

  // Records a transport/streaming error and ensures the connection closes, but does NOT settle the
  // final status itself — that is left to _onClose. If the socket is still open (a genuine local
  // failure), force it closed; if it is already closing/closed (e.g. the server sent a quota close),
  // leave it so the incoming close reason is preserved and wins.
  _abort(err) {
    if (!this._streamError) this._streamError = err;
    try {
      if (this.socket.readyState === this.socket.OPEN || this.socket.readyState === this.socket.CONNECTING) {
        this.socket.terminate();
      }
    } catch { /* ignore */ }
  }

  _sendFrame(chunk, fin) {
    return new Promise((resolve, reject) => {
      this.socket.send(chunk, { binary: true, fin }, (err) => (err ? reject(err) : resolve()));
    });
  }

  // Tails the file, sending newly appended bytes as non-final fragments until end-of-input is
  // signalled and the file is fully drained; then closes the binary message and sends FinishUpload.
  async _streamLoop() {
    let offset = 0;
    let handle = null;
    try {
      for (;;) {
        // The upload may have been failed/closed elsewhere (e.g. the timeout backstop, or an abort);
        // stop tailing instead of polling forever. The finally block closes the file handle.
        if (this.settled) return;
        if (!handle) {
          // The keyword may create the file after starting the upload; keep trying until it appears.
          // Only ENOENT means "not created yet"; any other error (permissions, fd limit, a path
          // component that is not a directory, ...) is a real problem and must fail the upload.
          try {
            handle = await fs.promises.open(this.filePath, 'r');
          } catch (err) {
            if (err.code !== 'ENOENT') throw err;
          }
        }
        let readSomething = false;
        if (handle) {
          const { size } = await handle.stat();
          if (size > offset) {
            // Read at most MAX_READ_CHUNK_BYTES per iteration; the loop drains the rest on the next
            // pass (readSomething -> continue), and the awaited send applies backpressure, so memory
            // stays bounded no matter how large or fast-growing the file is.
            const len = Math.min(size - offset, MAX_READ_CHUNK_BYTES);
            const buf = Buffer.allocUnsafe(len);
            const { bytesRead } = await handle.read(buf, 0, len, offset);
            if (bytesRead > 0) {
              const chunk = buf.subarray(0, bytesRead);
              this.md5.update(chunk);
              await this._sendFrame(chunk, false);
              offset += bytesRead;
              readSomething = true;
            }
          }
        }
        if (readSomething) continue; // drain as fast as the file grows before yielding
        if (this._eofDone) {
          if (this._eofError) throw this._eofError;
          if (handle === null) {
            // complete() was called but the file was never created: upload an empty resource, but
            // warn — pointing a live upload at a file that never appears is not expected.
            logger.warn(`Live reporting file upload: file '${this.filePath}' never appeared before completion; uploading an empty resource. Specifying a file that does not exist is not expected.`);
          }
          // Terminate the (possibly empty) binary message with a final, empty continuation frame.
          await this._sendFrame(Buffer.alloc(0), true);
          break;
        }
        // Wait for either more data (poll) or the end-of-input signal, whichever comes first.
        await Promise.race([delay(DEFAULT_FILE_POLL_INTERVAL_MS), this._eofPromise]);
      }
      if (this.settled) return; // socket may have been torn down between the final frame and now
      this.clientChecksum = this.md5.digest('hex');
      this.state = 'EXPECTING_ACK';
      this.socket.send(JSON.stringify({ '@': 'FinishUpload', checksum: this.clientChecksum }));
    } catch (err) {
      // Let _onClose decide the final status (preserves a server quota close reason).
      this._abort(err);
    } finally {
      if (handle) { try { await handle.close(); } catch { /* ignore */ } }
    }
  }

  _onMessage(data) {
    let msg;
    try {
      msg = JSON.parse(data.toString());
    } catch (e) {
      return this._fail(new Error('Received malformed message from streaming server: ' + e.message));
    }
    const type = msg['@'];
    if (this.state === 'CONNECTING' && type === 'ReadyForUpload') {
      this.reference = msg.reference;
      this.state = 'UPLOADING';
      this._streamLoop();
    } else if (this.state === 'EXPECTING_ACK' && type === 'UploadAcknowledged') {
      if (msg.checksum !== this.clientChecksum) {
        this._settleReject(new Error(`Checksum mismatch: client reported ${this.clientChecksum}, server reported ${msg.checksum}`));
        try { this.socket.close(WS_UNEXPECTED_CONDITION, 'checksum mismatch'); } catch { /* ignore */ }
        return;
      }
      this.state = 'FINALIZED';
      this.socket.close(WS_NORMAL_CLOSURE, CLOSEREASON_UPLOAD_COMPLETED);
      this._settleResolve({
        transferStatus: 'COMPLETED',
        size: msg.size,
        numberOfLines: msg.numberOfLines != null ? msg.numberOfLines : null,
        reference: this.reference,
      });
    } else {
      this._fail(new Error(`Unexpected message '${type}' in state ${this.state}`));
    }
  }

  _onClose(code, reasonBuf) {
    if (this.state === 'FINALIZED') return;
    const reason = reasonBuf ? reasonBuf.toString() : '';
    if (reason.startsWith(QUOTA_EXCEEDED_PREFIX)) {
      // Controller rejected the upload because of quota restrictions (mirrors Java QuotaExceededException).
      this._settleReject(new QuotaExceededError(reason.substring(QUOTA_EXCEEDED_PREFIX.length)));
    } else if (this._streamError) {
      this._settleReject(this._streamError);
    } else {
      this._settleReject(new Error(`WebSocket closed (code=${code}, reason='${reason}') before the upload completed`));
    }
  }

  // Signals end-of-input and returns the final-status promise (idempotent).
  complete() {
    if (!this._eofDone) {
      this._eofDone = true;
      this._eofResolve();
    }
    return this.finalStatus;
  }

  // Aborts the upload. Wakes the stream loop (if waiting) and terminates the connection.
  cancel(reason) {
    const err = reason instanceof Error ? reason : new Error(reason || 'Upload cancelled');
    if (!this._eofDone) {
      this._eofDone = true;
      this._eofError = err;
      this._eofResolve();
    }
    this._fail(err);
    return this.finalStatus;
  }
}

/**
 * Upload controller used when no controller streaming context is available (e.g. local runner
 * execution): it reads nothing onto the wire and reports DISABLED, mirroring Java's
 * DiscardingStreamingUploadProvider. Exposes the same controller interface as the WebSocket one.
 */
class DiscardingUploadController {
  constructor(filePath) {
    this.filePath = filePath;
    this.reference = null;
    this._done = false;
    this.finalStatus = new Promise((resolve) => { this._resolve = resolve; });
    this.finalStatus.catch(() => {});
  }

  async _settle() {
    if (this._done) return this.finalStatus;
    this._done = true;
    let size = 0;
    try {
      size = (await fs.promises.stat(this.filePath)).size;
    } catch (e) {
      logger.debug(`Discarding upload could not stat ${this.filePath}:`, e && e.message ? e.message : e);
    }
    this._resolve({ transferStatus: 'DISABLED', size, numberOfLines: null, reference: null });
    return this.finalStatus;
  }

  complete() { return this._settle(); }

  cancel() { return this._settle(); }
}

/**
 * Streaming file-upload channel exposed to keywords (Java: `step.streaming.client.upload.StreamingUploads`).
 *
 * Two usage styles:
 *  - one-shot: `await fileUploads.uploadBinaryFile(path)` uploads an already-produced file.
 *  - streaming: `const u = fileUploads.startBinaryFileUpload(path); ...write to path...; await u.complete()`
 *    streams the file while it is still being written.
 *
 * Uploads started but never completed by the keyword are cancelled when {@link close} is called.
 */
class StreamingUploads {
  constructor(uploader) {
    this.uploader = uploader;
    this.controllers = new Set();
  }

  _start(filePath, { mimeType, filename, supportsLineAccess }) {
    const metadata = {
      filename: filename || path.basename(filePath),
      mimeType,
      supportsLineAccess,
    };
    const controller = this.uploader.startUpload(filePath, metadata);
    this.controllers.add(controller);
    const forget = () => this.controllers.delete(controller);
    controller.finalStatus.then(forget, forget);
    return new StreamingUpload(controller);
  }

  /**
   * Starts a streaming upload of a binary file, returning a handle immediately. The file is streamed
   * as it grows; call {@link StreamingUpload#complete} when done writing.
   * @param {string} filePath
   * @param {{ mimeType?: string, filename?: string }} [options]
   * @returns {StreamingUpload}
   */
  startBinaryFileUpload(filePath, { mimeType = 'application/octet-stream', filename } = {}) {
    return this._start(filePath, { mimeType, filename, supportsLineAccess: false });
  }

  /**
   * Starts a streaming upload of a UTF-8 text file (enables line-by-line viewing in the Step UI).
   * @param {string} filePath
   * @param {{ mimeType?: string, filename?: string }} [options]
   * @returns {StreamingUpload}
   */
  startTextFileUpload(filePath, { mimeType = 'text/plain', filename } = {}) {
    return this._start(filePath, { mimeType, filename, supportsLineAccess: true });
  }

  /**
   * Convenience one-shot upload of an already-produced binary file.
   * @returns {Promise<object>} final transfer status
   */
  uploadBinaryFile(filePath, options) {
    return this.startBinaryFileUpload(filePath, options).complete();
  }

  /**
   * Convenience one-shot upload of an already-produced UTF-8 text file.
   * @returns {Promise<object>} final transfer status
   */
  uploadTextFile(filePath, options) {
    return this.startTextFileUpload(filePath, options).complete();
  }

  async close() {
    // Cancel uploads the keyword started but never completed, then wait for all to settle.
    for (const controller of this.controllers) {
      controller.cancel(new Error('Live reporting closed before the upload was completed'));
    }
    await Promise.allSettled([...this.controllers].map((c) => c.finalStatus));
  }
}

/**
 * Handle for an in-progress streaming upload (Java: `step.streaming.client.upload.StreamingUpload`).
 */
class StreamingUpload {
  constructor(controller) {
    this._controller = controller;
  }

  /** The streaming resource reference, available once the server has acknowledged the upload start. */
  get reference() {
    return this._controller.reference;
  }

  /**
   * Signals end-of-input and resolves with the final transfer status once the upload completes.
   * @returns {Promise<object>}
   */
  complete() {
    return this._controller.complete();
  }

  /**
   * Aborts the upload.
   * @param {Error|string} [reason]
   */
  cancel(reason) {
    return this._controller.cancel(reason);
  }
}

// Uploader (controller factory) backed by the WebSocket transport.
function createWebsocketUploader(WebSocket, endpointUri) {
  return {
    startUpload(filePath, metadata) {
      return new WebsocketUploadController(WebSocket, endpointUri, filePath, metadata);
    },
  };
}

// Uploader (controller factory) that discards everything (no controller context available).
const discardingUploader = {
  startUpload(filePath) {
    return new DiscardingUploadController(filePath);
  },
};

/**
 * Builds the WebSocket upload endpoint URI (Java: getWebsocketUploadUri).
 * Converts the http(s) reporting URL to ws(s), appends the upload path, and carries the context id.
 */
function buildWebsocketUploadUri(baseUrl, uploadPath, contextId) {
  const host = baseUrl.replace(/^http/, 'ws'); // http -> ws, https -> wss
  let p = uploadPath;
  while (p.startsWith('/')) p = p.substring(1);
  return `${host}/${p}?${STREAMING_UPLOAD_CONTEXT_ID}=${encodeURIComponent(contextId)}`;
}

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
 * Builds the live measure destination from the keyword properties, or a discarding one when no
 * controller context is present (local execution).
 */
function createMeasureDestination(properties) {
  const contextId = properties[LIVEREPORTING_CONTEXT_ID];
  if (!contextId) {
    return new DiscardingLiveMeasureDestination();
  }
  let baseUrl;
  try {
    baseUrl = getReportingUrl(properties);
  } catch (e) {
    logger.error('Could not resolve the live-reporting URL, measures will be discarded:', e);
    return new DiscardingLiveMeasureDestination();
  }
  if (!baseUrl) {
    return new DiscardingLiveMeasureDestination();
  }
  const measuresUrl = `${baseUrl}/rest/live-reporting/${contextId}/measures`;
  logger.debug(`Live reporting measures enabled, endpoint: ${measuresUrl}`);
  return new RestUploadingLiveMeasureDestination(measuresUrl);
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

/**
 * Builds the file-upload channel from the keyword properties, or a discarding one when no controller
 * streaming context is present (or the ws module is unavailable).
 */
function createFileUploads(properties) {
  const contextId = properties[STREAMING_UPLOAD_CONTEXT_ID];
  if (!contextId) {
    return new StreamingUploads(discardingUploader);
  }
  let baseUrl;
  try {
    baseUrl = getReportingUrl(properties);
  } catch (e) {
    logger.error('Could not resolve the live-reporting URL, file uploads will be discarded:', e);
    return new StreamingUploads(discardingUploader);
  }
  const uploadPath = properties[STREAMING_WEBSOCKET_UPLOAD_PATH];
  if (!baseUrl || !uploadPath) {
    return new StreamingUploads(discardingUploader);
  }
  const WebSocket = loadWebSocket();
  if (!WebSocket) {
    logger.warn('The ws module is not available; file uploads will be discarded');
    return new StreamingUploads(discardingUploader);
  }
  const endpointUri = buildWebsocketUploadUri(baseUrl, uploadPath, contextId);
  logger.debug(`Live reporting file uploads enabled, endpoint: ${baseUrl.replace(/^http/, 'ws')}/${uploadPath.replace(/^\/+/, '')}`);
  return new StreamingUploads(createWebsocketUploader(WebSocket, endpointUri));
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
  LiveMeasures,
  RestUploadingLiveMeasureDestination,
  DiscardingLiveMeasureDestination,
  LiveMetrics,
  RestUploadingLiveMetricDestination,
  DiscardingLiveMetricDestination,
  MetricSamplesCollector,
  CounterMetric,
  GaugeMetric,
  HistogramMetric,
  InstrumentType,
  StreamingUploads,
  StreamingUpload,
  QuotaExceededError,
};
