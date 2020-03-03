const runner = require('../api/runner/runner')({'Property1': 'Prop1'})
const assert = require('assert')

;(async () => {
  // Test the happy path
  var output = await runner.run('Echo', {Param1: 'Val1'})
  assert.equal(output.payload.Param1, 'Val1')
  assert.equal(output.payload.properties.Property1, 'Prop1')

  // Test the method output.setError
  var errorMsg = 'MyError'
  output = await runner.run('SetErrorTestKW', {ErrorMsg: errorMsg, rethrow_error: true})
  assert.equal(output.error.msg, errorMsg)
  assert.equal(output.error.type, 'TECHNICAL')

  // Test the method output.setError with an exception as argument
  errorMsg = 'MyError2'
  output = await runner.run('SetErrorWithExceptionKW', {ErrorMsg: errorMsg, rethrow_error: true})
  assert.equal(output.error.msg, errorMsg)

  // Test the method output.setError with an error message and an exception as argument
  errorMsg = 'MyError3'
  output = await runner.run('SetErrorWithMessageAndExceptionKW', {ErrorMsg: errorMsg, rethrow_error: true})
  assert.equal(output.error.msg, errorMsg)
  assert.equal(output.attachments.length, 1)

  // Test the method output.fail
  errorMsg = 'MyError4'
  output = await runner.run('FailKW', {ErrorMsg: errorMsg, rethrow_error: true})
  assert.equal(output.error.msg, errorMsg)

  // Test the method output.setBusinessError
  errorMsg = 'MyBusinessError'
  output = await runner.run('BusinessErrorTestKW', {ErrorMsg: errorMsg, rethrow_error: true})
  assert.equal(output.error.msg, errorMsg)

  // Test onError hook
  global.isOnErrorCalled = false
  const errorMsg1 = 'Error - rethrow'
  const output1 = await runner.run('ErrorTestKW', {ErrorMsg: errorMsg1, rethrow_error: true})
  assert.equal(output1.error.msg, errorMsg1)
  assert.equal(global.isOnErrorCalled, true)

  // Test onError hook with no rethrow
  global.isOnErrorCalled = false
  const errorMsg2 = 'Error - do not rethrow'
  const output2 = await runner.run('ErrorTestKW', {ErrorMsg: errorMsg2, rethrow_error: false})
  assert.equal(output2.error, undefined)
  assert.equal(global.isOnErrorCalled, true)

  // Test rejected promises
  output = await runner.run('ErrorRejectedPromiseTestKW', {Param1: 'Val1'})
  assert.equal(output.error, undefined)

  // Test uncaught exceptions
  output = await runner.run('ErrorUncaughtExceptionTestKW', {Param1: 'Val1'})
  assert.equal(output.error, undefined)

  console.log('PASSED')
})()
