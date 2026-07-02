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
 * Measures channel: live performance (response-time) measures, batched and POSTed to the controller's
 * /rest/live-reporting/{contextId}/measures endpoint.
 */

// MeasureStatus and its validation are shared with the (final) OutputBuilder measures, so the
// live and end-of-keyword measure APIs behave identically.
const { MeasureStatus } = require('../output');
const { logger, LIVEREPORTING_CONTEXT_ID, getReportingUrl, BatchingRestPoster } = require('./shared');

const VALID_STATUSES = new Set(Object.values(MeasureStatus));

function assertValidStatus(status) {
  if (status !== undefined && !VALID_STATUSES.has(status)) {
    throw new TypeError(`Invalid measure status: "${status}". Must be one of: ${[...VALID_STATUSES].join(', ')}`);
  }
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

module.exports = {
  DiscardingLiveMeasureDestination,
  RestUploadingLiveMeasureDestination,
  LiveMeasures,
  createMeasureDestination,
};
