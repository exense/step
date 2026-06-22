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

// Fires after the setImmediate flush (timer phase comes after check phase in the *next* iteration),
// so the error lands between keywords or after the last keyword — not in the triggering keyword's output.
exports.FireAndForgetRejectionKW = async (input, output, session, properties) => {
  setTimeout(() => Promise.reject(new Error('inter-keyword rejection')), 50)
}

exports.FireAndForgetExceptionKW = async (input, output, session, properties) => {
  setTimeout(() => { throw new Error('inter-keyword exception') }, 50)
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

// --- live reporting (measures) ---

exports.LiveMeasureKW = async (input, output, session, properties, liveReporting) => {
  output.add('hasLiveReporting', !!(liveReporting && liveReporting.measures))
  liveReporting.measures.startMeasure('live-step')
  liveReporting.measures.stopMeasure({ status: 'PASSED', data: { k: 'v' } })
  liveReporting.measures.addMeasure('live-pre-timed', 42, { status: 'FAILED' })
}

exports.UploadFileKW = async (input, output, session, properties, liveReporting) => {
  const status = await liveReporting.fileUploads.uploadBinaryFile(input.filePath, { mimeType: 'text/plain' })
  output.add('transferStatus', status.transferStatus)
  output.add('size', status.size)
  output.add('reference', status.reference)
}

// Streams a file while it is still being written: start the upload, append chunks over time, then complete.
exports.StreamGrowingFileKW = async (input, output, session, properties, liveReporting) => {
  const fs = require('fs')
  fs.writeFileSync(input.filePath, '')
  const upload = liveReporting.fileUploads.startTextFileUpload(input.filePath)
  for (const chunk of input.chunks) {
    fs.appendFileSync(input.filePath, chunk)
    await new Promise(r => setTimeout(r, 30))
  }
  const status = await upload.complete()
  output.add('transferStatus', status.transferStatus)
  output.add('size', status.size)
}

exports.LiveMetricKW = async (input, output, session, properties, liveReporting) => {
  const counter = liveReporting.metrics.registerCounter('requests', { endpoint: '/login' })
  counter.increment()
  counter.increment(3)
  const gauge = liveReporting.metrics.registerGauge('queueDepth')
  gauge.observe(7)
  const histogram = liveReporting.metrics.registerHistogram('respTimeMs')
  histogram.observe(12)
  histogram.observe(25)
  output.add('ok', true)
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

// --- session auto-disposal ---

exports.StoreCloseableKW = async (input, output, session) => {
  session.set('resource', {
    close() { require('fs').writeFileSync(input['closePath'], 'closed') }
  })
}

// --- backward compat: output.send() is no longer required but must still work ---

exports.SendNoArgCompatKW = async (input, output) => {
  output.add('result', 'ok')
  output.send()
}

exports.SendWithPayloadCompatKW = async (input, output) => {
  output.send({ result: 'ok' })
}
