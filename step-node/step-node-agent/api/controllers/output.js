module.exports = function OutputBuilder (callback) {
  let exports = {}

  exports.builder = { payload: { attachments: [], payload: {} }, attachments: [] }

  exports.send = function (payload) {
    exports.builder.payload.payload = payload
    if (callback) {
      callback(exports.builder)
    }
  }

  function buildDefaultTechnicalError (message) {
    return { msg: message, type: 'TECHNICAL', root: true, code: 0 }
  }

  function buildDefaultBusinessError (message) {
    return { msg: message, type: 'BUSINESS', root: true, code: 0 }
  }

  function attachException (e) {
    exports.attach(
      {
        'name': 'exception.log',
        'isDirectory': false,
        'description': 'exception stacktrace from keyword',
        'hexContent': Buffer.from(e.stack).toString('base64')
      })
  }

  exports.fail = function (arg1, arg2) {
    exports.setError(arg1, arg2)
    if (callback) {
      callback(exports.builder)
    }
  }

  exports.setError = function (arg1, arg2) {
    if (typeof arg1 === 'string' || arg1 instanceof String) {
      exports.builder.payload.error = buildDefaultTechnicalError(arg1)
      if (arg2 && arg2 instanceof Error) {
        attachException(arg2)
      }
    } else if (arg1 instanceof Error) {
      exports.builder.payload.error = buildDefaultTechnicalError(arg1.message)
      attachException(arg1)
    } else if (typeof arg1 === 'object') {
      exports.builder.payload.error = arg1
    }
  }

  exports.setBusinessError = function (errorMessage) {
    exports.builder.payload.error = buildDefaultBusinessError(errorMessage)
  }

  exports.attach = function (attachment) {
    exports.builder.payload.attachments.push(attachment)
  }

  return exports
}
