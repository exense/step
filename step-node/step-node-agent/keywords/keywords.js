exports.Echo = async (input, output, session, properties) => {
  input['properties'] = properties
  output.send(input)
}

exports.ErrorTestKW = async (input, output, session, properties) => {
  throw new Error(input['ErrorMsg'])
}

exports.SetErrorTestKW = async (input, output, session, properties) => {
  output.setError(input['ErrorMsg'])
  output.send()
}

exports.SetErrorWithExceptionKW = async (input, output, session, properties) => {
  output.setError(new Error(input['ErrorMsg']))
  output.send()
}

exports.SetErrorWithMessageAndExceptionKW = async (input, output, session, properties) => {
  output.setError(input['ErrorMsg'], new Error(input['ErrorMsg']))
  output.send()
}

exports.FailKW = async (input, output, session, properties) => {
  output.fail(input['ErrorMsg'])
}

exports.BusinessErrorTestKW = async (input, output, session, properties) => {
  output.setBusinessError(input['ErrorMsg'])
  output.send()
}

exports.ErrorRejectedPromiseTestKW = async (input, output, session, properties) => {
  Promise.reject(new Error('test'))
  output.send()
}

exports.ErrorUncaughtExceptionTestKW = async (input, output, session, properties) => {
  process.nextTick(function () {
    throw new Error()
  })
  output.send()
}

exports.onError = async (exception, input, output, session, properties) => {
  console.log('[onError] Exception is: \'' + exception + '\'')
  global.isOnErrorCalled = true
  return input['rethrow_error']
}
