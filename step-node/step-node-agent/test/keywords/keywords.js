exports.Echo = async (input, output, session, properties) => {
  input['properties'] = properties
  Object.entries(input).forEach(([k, v]) => output.add(k, v))
}

exports.ErrorTestKW = async (input, output, session, properties) => {
  throw new Error(input['ErrorMsg'])
}

exports.SetErrorTestKW = async (input, output, session, properties) => {
  output.setError(input['ErrorMsg'])
}

exports.SetErrorWithExceptionKW = async (input, output, session, properties) => {
  output.setError(new Error(input['ErrorMsg']))
}

exports.SetErrorWithMessageAndExceptionKW = async (input, output, session, properties) => {
  output.setError(input['ErrorMsg'], new Error(input['ErrorMsg']))
}

exports.FailKW = async (input, output, session, properties) => {
  output.fail(input['ErrorMsg'])
}

exports.BusinessErrorTestKW = async (input, output, session, properties) => {
  output.setBusinessError(input['ErrorMsg'])
}

exports.ErrorRejectedPromiseTestKW = async (input, output, session, properties) => {
  Promise.reject(new Error('test'))
}

exports.ErrorUncaughtExceptionTestKW = async (input, output, session, properties) => {
  process.nextTick(function () {
    throw new Error()
  })
}

exports.onError = async (exception, input, output, session, properties) => {
  console.log('[onError] Exception is: \'' + exception + '\'')
  output.builder.payload.payload.onErrorCalled = true
  return input['rethrow_error']
}

// --- output.add ---

exports.AddKW = async (input, output, session, properties) => {
  output.add('name', 'Alice').add('score', 42).add('active', true)
}

// --- output.appendError ---

exports.AppendErrorToExistingKW = async (input, output, session, properties) => {
  output.setError('base error').appendError(' + extra detail')
}

exports.AppendErrorToNoneKW = async (input, output, session, properties) => {
  output.appendError('fresh error')
}

// --- output.attach ---

exports.AttachKW = async (input, output, session, properties) => {
  output.attach({ name: 'report.txt', isDirectory: false, description: 'test attachment', hexContent: Buffer.from('hello').toString('base64') })
}

// --- measurement methods ---

exports.StartStopMeasureKW = async (input, output, session, properties) => {
  output.startMeasure('step1')
  await new Promise(r => setTimeout(r, 10))
  output.stopMeasure()
}

exports.StartStopMeasureWithStatusKW = async (input, output, session, properties) => {
  output.startMeasure('failing-step')
  output.stopMeasure({ status: 'FAILED', data: { reason: 'assertion failed' } })
}

exports.AddMeasureKW = async (input, output, session, properties) => {
  output.addMeasure('pre-timed', 150, { status: 'TECHNICAL_ERROR', begin: Date.now() - 150, data: { info: 'test' } })
}

exports.MultipleMeasuresKW = async (input, output, session, properties) => {
  output.startMeasure('first')
  output.stopMeasure()
  output.startMeasure('second')
  output.stopMeasure({ status: 'FAILED' })
  output.addMeasure('third', 50)
}

// --- session ---

exports.SessionSetKW = async (input, output, session) => {
  session.set('sharedKey', input['value'])
}

exports.SessionGetKW = async (input, output, session) => {
  output.add('value', session.get('sharedKey'))
}

exports.SessionSetDotKW = async (input, output, session) => {
  session.dotKey = input['value']
}

exports.SessionGetDotKW = async (input, output, session) => {
  output.add('value', session.dotKey)
}

// --- beforeKeyword / afterKeyword hook tracking ---

let _hookCalls = []

exports.beforeKeyword = async (functionName) => {
  _hookCalls.push(`before:${functionName}`)
}

exports.afterKeyword = async (functionName) => {
  _hookCalls.push(`after:${functionName}`)
}

exports.GetHookCallsKW = async (input, output) => {
  const calls = [..._hookCalls]
  _hookCalls = []
  output.add('calls', calls)
}

// --- properties ---

exports.GetPropertyKW = async (input, output, session, properties) => {
  output.add('value', properties[input['key']])
}

// --- backward compat: output.send() is no longer required but must still work ---

exports.SendNoArgCompatKW = async (input, output) => {
  output.add('result', 'ok')
  output.send()
}

exports.SendWithPayloadCompatKW = async (input, output) => {
  output.send({ result: 'ok' })
}
