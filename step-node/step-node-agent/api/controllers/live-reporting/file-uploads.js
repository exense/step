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
 * File-uploads channel (Java: step.streaming WebsocketUploadClient): streams files/resources to the
 * controller over WebSocket, including files that are still being written.
 *
 *   ws(s)://{host}/{uploadPath}?streamingUploadContextId={id}
 *   StartUpload -> ReadyForUpload -> (one binary message, fragmented) -> FinishUpload -> UploadAcknowledged -> close
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { logger, getReportingUrl } = require('./shared');

// Property keys injected by the controller (Java: LiveReportingConstants / StreamingResourceUploadContext).
const STREAMING_WEBSOCKET_UPLOAD_PATH = '$liveReporting.streaming.websocket.upload.path';
const STREAMING_UPLOAD_CONTEXT_ID = 'streamingUploadContextId';
// Env var through which the parent agent passes the absolute path of its bundled `ws` module to the
// forked keyword process (whose own require() resolves against the keyword project, not the agent).
const WS_MODULE_ENV = 'STEP_AGENT_WS_MODULE';

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

module.exports = {
  QuotaExceededError,
  StreamingUploads,
  StreamingUpload,
  discardingUploader,
  createFileUploads,
};
