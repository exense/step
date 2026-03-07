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
  output.builder.payload.payload.onErrorCalled = true
  return input['rethrow_error']
}

// --- output.add ---

exports.AddKW = async (input, output, session, properties) => {
  output.add('name', 'Alice').add('score', 42).add('active', true).send()
}

// --- output.appendError ---

exports.AppendErrorToExistingKW = async (input, output, session, properties) => {
  output.setError('base error').appendError(' + extra detail').send()
}

exports.AppendErrorToNoneKW = async (input, output, session, properties) => {
  output.appendError('fresh error').send()
}

// --- output.attach ---

exports.AttachKW = async (input, output, session, properties) => {
  output.attach({ name: 'report.txt', isDirectory: false, description: 'test attachment', hexContent: Buffer.from('hello').toString('base64') })
  output.send()
}

// --- measurement methods ---

exports.StartStopMeasureKW = async (input, output, session, properties) => {
  output.startMeasure('step1')
  await new Promise(r => setTimeout(r, 10))
  output.stopMeasure()
  output.send()
}

exports.StartStopMeasureWithStatusKW = async (input, output, session, properties) => {
  output.startMeasure('failing-step')
  output.stopMeasure({ status: 'FAILED', data: { reason: 'assertion failed' } })
  output.send()
}

exports.AddMeasureKW = async (input, output, session, properties) => {
  output.addMeasure('pre-timed', 150, { status: 'TECHNICAL_ERROR', begin: Date.now() - 150, data: { info: 'test' } })
  output.send()
}

exports.MultipleMeasuresKW = async (input, output, session, properties) => {
  output.startMeasure('first')
  output.stopMeasure()
  output.startMeasure('second')
  output.stopMeasure({ status: 'FAILED' })
  output.addMeasure('third', 50)
  output.send()
}
