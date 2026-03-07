const runner = require('../api/runner/runner')({'Property1': 'Prop1'})
const assert = require('assert')
const { OutputBuilder, MeasureStatus } = require('../api/controllers/output')

;(async () => {
  try {
    // Test the happy path
    let output = await runner.run('Echo', {Param1: 'Val1'})
    assert.equal(output.payload.Param1, 'Val1')
    assert.equal(output.payload.properties.Property1, 'Prop1')

    // Test the method output.setError
    let errorMsg = 'MyError'
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
    const errorMsg1 = 'Error - rethrow'
    const output1 = await runner.run('ErrorTestKW', {ErrorMsg: errorMsg1, rethrow_error: true})
    assert.equal(output1.error.msg, errorMsg1)
    assert.equal(output1.payload.onErrorCalled, true)

    // Test onError hook with no rethrow
    const errorMsg2 = 'Error - do not rethrow'
    const output2 = await runner.run('ErrorTestKW', {ErrorMsg: errorMsg2, rethrow_error: false})
    assert.equal(output2.error, undefined)
    assert.equal(output1.payload.onErrorCalled, true)

    // Test rejected promises
    output = await runner.run('ErrorRejectedPromiseTestKW', {Param1: 'Val1'})
    assert.equal(output.error, undefined)

    // Test uncaught exceptions
    output = await runner.run('ErrorUncaughtExceptionTestKW', {Param1: 'Val1'})
    assert.equal(output.error, undefined)

    // Test not existing keyword
    output = await runner.run('Not existing Keyword', {})
    assert.equal(output.error.msg, "Unable to find Keyword 'Not existing Keyword'")

    // --- output.add ---

    // Test building payload incrementally with add()
    output = await runner.run('AddKW', {})
    assert.equal(output.payload.name, 'Alice')
    assert.equal(output.payload.score, 42)
    assert.equal(output.payload.active, true)

    // --- output.appendError ---

    // Test appending to an existing error
    output = await runner.run('AppendErrorToExistingKW', {})
    assert.equal(output.error.msg, 'base error + extra detail')
    assert.equal(output.error.type, 'TECHNICAL')

    // Test appendError creating a new error when none exists
    output = await runner.run('AppendErrorToNoneKW', {})
    assert.equal(output.error.msg, 'fresh error')
    assert.equal(output.error.type, 'TECHNICAL')

    // --- output.attach ---

    output = await runner.run('AttachKW', {})
    assert.equal(output.attachments.length, 1)
    assert.equal(output.attachments[0].name, 'report.txt')
    assert.equal(output.attachments[0].isDirectory, false)

    // --- measurement methods ---

    // Test startMeasure / stopMeasure with default PASSED status
    output = await runner.run('StartStopMeasureKW', {})
    assert.equal(output.measures.length, 1)
    assert.equal(output.measures[0].name, 'step1')
    assert.equal(output.measures[0].status, MeasureStatus.PASSED)
    assert.ok(output.measures[0].duration >= 10, 'duration should be at least the sleep time')
    assert.ok(typeof output.measures[0].begin === 'number')

    // Test stopMeasure with explicit FAILED status and custom data
    output = await runner.run('StartStopMeasureWithStatusKW', {})
    assert.equal(output.measures[0].name, 'failing-step')
    assert.equal(output.measures[0].status, MeasureStatus.FAILED)
    assert.equal(output.measures[0].data.reason, 'assertion failed')

    // Test addMeasure with pre-set duration and TECHNICAL_ERROR status
    output = await runner.run('AddMeasureKW', {})
    assert.equal(output.measures[0].name, 'pre-timed')
    assert.equal(output.measures[0].duration, 150)
    assert.equal(output.measures[0].status, MeasureStatus.TECHNICAL_ERROR)
    assert.equal(output.measures[0].data.info, 'test')
    assert.ok(typeof output.measures[0].begin === 'number')

    // Test multiple measures accumulated in one keyword call
    output = await runner.run('MultipleMeasuresKW', {})
    assert.equal(output.measures.length, 3)
    assert.equal(output.measures[0].name, 'first')
    assert.equal(output.measures[0].status, MeasureStatus.PASSED)
    assert.equal(output.measures[1].name, 'second')
    assert.equal(output.measures[1].status, MeasureStatus.FAILED)
    assert.equal(output.measures[2].name, 'third')
    assert.equal(output.measures[2].duration, 50)

    // --- MeasureStatus validation (inline, no fork) ---

    // stopMeasure rejects unknown status strings
    const ob = new OutputBuilder(null)
    ob.startMeasure('test')
    assert.throws(
      () => ob.stopMeasure({ status: 'INVALID_STATUS' }),
      (err) => err instanceof TypeError && err.message.includes('INVALID_STATUS')
    )

    // addMeasure rejects unknown status strings
    assert.throws(
      () => ob.addMeasure('test', 100, { status: 'WRONG' }),
      (err) => err instanceof TypeError && err.message.includes('WRONG')
    )

    // All valid MeasureStatus values are accepted without throwing
    const ob2 = new OutputBuilder(null)
    for (const status of Object.values(MeasureStatus)) {
      ob2.startMeasure('check')
      assert.doesNotThrow(() => ob2.stopMeasure({ status }))
    }

    console.log('PASSED')
  } finally {
    runner.close();
  }
})()
