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
 * Shared infrastructure for the live-reporting channels: the logger, the reporting-URL resolution,
 * and the batching REST poster used by both the measures and metrics channels.
 */

const http = require('http');
const https = require('https');

// In the forked keyword process these files are copied next to their dependencies (agent-fork-libs),
// where the agent logger is not reachable. Fall back to the console in that case (same as session.js).
let logger;
try {
  logger = require('../../logger').child({ component: 'LiveReporting' });
} catch {
  logger = { debug: console.debug.bind(console), info: console.log.bind(console), warn: console.warn.bind(console), error: console.error.bind(console) };
}

// Property keys injected by the controller (Java: LiveReportingConstants).
const LIVEREPORTING_CONTEXT_ID = '$liveReporting.contextId';
const LIVEREPORTING_CONTROLLER_URL = '$liveReporting.controllerUrl';
// Agent-side override of the reporting base URL (Java: getReportingUrl).
const REPORTING_URL_AGENT_OVERRIDE = 'step.reporting.url';

const DEFAULT_BATCH_SIZE = 500;
const DEFAULT_FLUSH_INTERVAL_MS = 5000;
const DEFAULT_READ_TIMEOUT_MS = 30_000;

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

module.exports = {
  logger,
  LIVEREPORTING_CONTEXT_ID,
  getReportingUrl,
  BatchingRestPoster,
};
