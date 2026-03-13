const MeasureStatus = Object.freeze({
  PASSED: 'PASSED',
  FAILED: 'FAILED',
  TECHNICAL_ERROR: 'TECHNICAL_ERROR',
});

const VALID_STATUSES = new Set(Object.values(MeasureStatus));

function assertValidStatus(status) {
  if (status !== undefined && !VALID_STATUSES.has(status)) {
    throw new TypeError(`Invalid measure status: "${status}". Must be one of: ${[...VALID_STATUSES].join(', ')}`);
  }
}

class OutputBuilder {
  constructor() {
    this.builder = { payload: { attachments: [], payload: {} }, attachments: [] }
    this._currentMeasure = null;
  }

  /**
   * Adds an output attribute to the payload. Can be called multiple times to build the payload
   * incrementally. Call send() with no arguments afterwards to flush.
   * @param {string} name
   * @param {*} value
   * @returns {OutputBuilder} this, for chaining
   */
  add(name, value) {
    this.builder.payload.payload[name] = value;
    return this;
  }

  /**
   * Sends the output. If a payload object is provided it replaces the current payload entirely.
   * If called with no arguments, the payload built up via add() is used.
   * @param {object} [payload]
   */
  send(payload) {
    if (payload !== undefined) {
      this.builder.payload.payload = payload;
    }
  }

  merge(output) {
    this.builder.payload = output;
  }

  fail(arg1, arg2) {
    this.setError(arg1, arg2)
  }

  build() {
    return this.builder;
  }

  /**
   * Sets a technical error, replacing any existing error.
   * Accepts a string message, an Error object, or a raw error object.
   * When a string + Error are passed the exception stack is attached.
   * @returns {OutputBuilder} this, for chaining
   */
  setError(arg1, arg2) {
    if (typeof arg1 === 'string' || arg1 instanceof String) {
      this.builder.payload.error = buildDefaultTechnicalError(String(arg1))
      if (arg2 && arg2 instanceof Error) {
        this.#attachException(arg2)
      }
    } else if (arg1 instanceof Error) {
      this.builder.payload.error = buildDefaultTechnicalError(arg1.message)
      this.#attachException(arg1)
    } else if (typeof arg1 === 'object') {
      this.builder.payload.error = arg1
    }
    return this;
  }

  /**
   * Appends a message to the existing technical error, or creates one if none exists.
   * @param {string} message
   * @returns {OutputBuilder} this, for chaining
   */
  appendError(message) {
    if (this.builder.payload.error) {
      this.builder.payload.error.msg = (this.builder.payload.error.msg || '') + message;
    } else {
      this.builder.payload.error = buildDefaultTechnicalError(message);
    }
    return this;
  }

  hasError() {
    return this.builder.payload.error;
  }

  /**
   * Sets a business error (results in FAILED status rather than ERROR in Step).
   * @param {string} errorMessage
   * @returns {OutputBuilder} this, for chaining
   */
  setBusinessError(errorMessage) {
    this.builder.payload.error = buildDefaultBusinessError(errorMessage)
    return this;
  }

  /**
   * Adds an attachment to the output.
   * @param {object} attachment
   */
  attach(attachment) {
    this.builder.payload.attachments.push(attachment)
  }

  // --- Measurement methods ---

  /**
   * Starts a measurement with the given name. Must be followed by stopMeasure().
   * @param {string} id - the measurement name
   * @param {number} [begin=Date.now()] - explicit start timestamp in ms
   */
  startMeasure(id, begin = Date.now()) {
    this._currentMeasure = { id, begin };
  }

  /**
   * Stops the current measurement started by startMeasure() and records it.
   * @param {{ status?: MeasureStatus, data?: object }} [options]
   */
  stopMeasure({ status = MeasureStatus.PASSED, data } = {}) {
    assertValidStatus(status);
    if (!this._currentMeasure) return;
    const duration = Date.now() - this._currentMeasure.begin;
    this.addMeasure(this._currentMeasure.id, duration, { begin: this._currentMeasure.begin, status, data });
    this._currentMeasure = null;
  }

  /**
   * Adds a pre-timed measurement directly.
   * @param {string} name
   * @param {number} durationMillis
   * @param {{ begin?: number, data?: object, status?: MeasureStatus }} [options]
   */
  addMeasure(name, durationMillis, { begin, data, status = MeasureStatus.PASSED } = {}) {
    assertValidStatus(status);
    if (!this.builder.payload.measures) {
      this.builder.payload.measures = [];
    }
    const measure = { name, duration: durationMillis, status };
    if (begin !== undefined) measure.begin = begin;
    if (data !== undefined) measure.data = data;
    this.builder.payload.measures.push(measure);
  }

  // --- Private helpers ---

  #attachException(e) {
    this.attach({
      'name': 'exception.log',
      'isDirectory': false,
      'description': 'exception stacktrace from keyword',
      'hexContent': Buffer.from(e.stack).toString('base64')
    })
  }
}

function buildDefaultTechnicalError(message) {
  return { msg: message, type: 'TECHNICAL', root: true, code: 0 }
}

function buildDefaultBusinessError(message) {
  return { msg: message, type: 'BUSINESS', root: true, code: 0 }
}

module.exports = { OutputBuilder, MeasureStatus };
