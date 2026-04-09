// Simulates bundler-compiled CJS (Bun, esbuild, webpack) where all exports
// are wrapped in a single `module.exports` object.  Node's import() puts this
// on `.default` instead of hoisting individual named exports.

var keywords_exports = {};

keywords_exports.BundledEcho = async (input, output, session, properties) => {
  input['properties'] = properties
  Object.entries(input).forEach(([k, v]) => output.add(k, v))
}

keywords_exports.onError = async (exception, input, output, session, properties) => {
  output.builder.payload.payload.onErrorCalled = true
  return input['rethrow_error']
}

keywords_exports.BundledErrorKW = async (input, output, session, properties) => {
  throw new Error(input['ErrorMsg'])
}

module.exports = keywords_exports;
