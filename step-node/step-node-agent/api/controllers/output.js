class OutputBuilder {
  constructor(callback) {
    this.callback = callback;
    this.builder = { payload: { attachments: [], payload: {} }, attachments: [] }
  }

  send(payload) {
    this.builder.payload.payload = payload
    if (this.callback) {
      this.callback(this.builder)
    }
  }

  merge(output) {
    this.builder.payload = output;
    if (this.callback) {
      this.callback(this.builder)
    }
  }

  fail(arg1, arg2) {
    this.setError(arg1, arg2)
    if (this.callback) {
      this.callback(this.builder)
    }
  }

  setError(arg1, arg2) {
    if (typeof arg1 === 'string' || arg1 instanceof String) {
      this.builder.payload.error = buildDefaultTechnicalError(arg1)
      if (arg2 && arg2 instanceof Error) {
        this.#attachException(arg2)
      }
    } else if (arg1 instanceof Error) {
      this.builder.payload.error = buildDefaultTechnicalError(arg1.message)
      this.#attachException(arg1)
    } else if (typeof arg1 === 'object') {
      this.builder.payload.error = arg1
    }
  }

  setBusinessError(errorMessage) {
    this.builder.payload.error = buildDefaultBusinessError(errorMessage)
  }

  attach(attachment) {
    this.builder.payload.attachments.push(attachment)
  }

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

module.exports = OutputBuilder;
