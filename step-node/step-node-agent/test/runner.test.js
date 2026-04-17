const { OutputBuilder, MeasureStatus } = require('../api/controllers/output')

describe('runner', () => {
  let runner

  // For performance reasons we reuse the same runner instance for most of the tests.
  // The tests that corrupt the runner with uncaught errors use their own runner
  beforeAll(() => {
    runner = require('../api/runner/runner')({ Property1: 'Prop1' })
    runner.setThrowExceptionOnError(false)
  })

  afterAll(async () => {
    await runner.close()
  })

  // ---------------------------------------------------------------------------
  // Happy path
  // ---------------------------------------------------------------------------

  test('Echo KW returns input param and agent property', async () => {
    const output = await runner.run('Echo', { Param1: 'Val1' })
    expect(output.payload.Param1).toBe('Val1')
    expect(output.payload.properties.Property1).toBe('Prop1')
  })

  // ---------------------------------------------------------------------------
  // Error-setting methods
  // ---------------------------------------------------------------------------

  describe('output.setError', () => {
    test('sets error message and TECHNICAL type', async () => {
      const output = await runner.run('SetErrorTestKW', { ErrorMsg: 'MyError', rethrow_error: true })
      expect(output.error.msg).toBe('MyError')
      expect(output.error.type).toBe('TECHNICAL')
    })

    test('accepts an exception as argument', async () => {
      const output = await runner.run('SetErrorWithExceptionKW', { ErrorMsg: 'MyError2', rethrow_error: true })
      expect(output.error.msg).toBe('MyError2')
    })

    test('accepts a message and exception, attaches stack trace', async () => {
      const output = await runner.run('SetErrorWithMessageAndExceptionKW', { ErrorMsg: 'MyError3', rethrow_error: true })
      expect(output.error.msg).toBe('MyError3')
      expect(output.attachments.find(a => a.name === 'exception.log')).toBeDefined()
    })
  })

  test('output.fail sets error message', async () => {
    const output = await runner.run('FailKW', { ErrorMsg: 'MyError4', rethrow_error: true })
    expect(output.error.msg).toBe('MyError4')
  })

  test('output.setBusinessError sets error message', async () => {
    const output = await runner.run('BusinessErrorTestKW', { ErrorMsg: 'MyBusinessError', rethrow_error: true })
    expect(output.error.msg).toBe('MyBusinessError')
  })

  // ---------------------------------------------------------------------------
  // onError hook
  // ---------------------------------------------------------------------------

  describe('onError hook', () => {
    test('is called and error is propagated when rethrow_error=true', async () => {
      const output = await runner.run('ErrorTestKW', { ErrorMsg: 'Error - rethrow', rethrow_error: true })
      expect(output.error.msg).toBe('Error - rethrow')
      expect(output.payload.onErrorCalled).toBe(true)
    })

    test('is called and error is suppressed when rethrow_error=false', async () => {
      const output = await runner.run('ErrorTestKW', { ErrorMsg: 'Error - do not rethrow', rethrow_error: false })
      expect(output.error).toBeUndefined()
      expect(output.payload.onErrorCalled).toBe(true)
    })
  })

  // ---------------------------------------------------------------------------
  // Unhandled async errors
  // ---------------------------------------------------------------------------

  describe('uncaught errors', () => {
    let runner

    // These tests corrupt the runner with uncaught errors. They create their own runner instance
    beforeEach(() => {
      runner = require('../api/runner/runner')({Property1: 'Prop1'})
      runner.setThrowExceptionOnError(false)
    })

    afterEach(async () => {
      await runner.close()
    })

    test('unhandled promise rejections surface as output error', async () => {
      const output = await runner.run('ErrorRejectedPromiseTestKW', { Param1: 'Val1' })
      expect(output.error).toBeDefined()
      expect(output.error.msg).toContain('Unhandled promise rejection')
    })

    test('uncaught exceptions surface as output error', async () => {
      const output = await runner.run('ErrorUncaughtExceptionTestKW', { Param1: 'Val1' })
      expect(output.error).toBeDefined()
      expect(output.error.msg).toContain('Uncaught exception')
    })
  })

  // ---------------------------------------------------------------------------
  // Inter-keyword uncaught errors
  // Errors fired via setTimeout(50) land after the setImmediate flush, so they
  // are NOT caught by the triggering keyword but by the next keyword's snapshot.
  // ---------------------------------------------------------------------------

  describe('inter-keyword uncaught errors', () => {
    let runner

    beforeEach(() => {
      runner = require('../api/runner/runner')()
      runner.setThrowExceptionOnError(false)
    })

    afterEach(async () => {
      // Echo consumed the inter-keyword error, so close() should succeed.
      // The try/catch guards against test pollution if a previous assertion failed.
      try { await runner.close() } catch { /* ignore */ }
    })

    test('unhandled rejection between keywords is attributed to the previous keyword', async () => {
      await runner.run('FireAndForgetRejectionKW', {})
      // Wait for the rejection to fire inside the fork (setTimeout 50 ms + margin).
      await new Promise(r => setTimeout(r, 100))
      const output = await runner.run('Echo', {})
      expect(output.error).toBeDefined()
      expect(output.error.msg).toContain('Unhandled promise rejection from a previous keyword')
      expect(output.error.msg).toContain('inter-keyword rejection')
    })

    test('uncaught exception between keywords is attributed to the previous keyword', async () => {
      await runner.run('FireAndForgetExceptionKW', {})
      // Wait for the exception to fire inside the fork (setTimeout 50 ms + margin).
      await new Promise(r => setTimeout(r, 100))
      const output = await runner.run('Echo', {})
      expect(output.error).toBeDefined()
      expect(output.error.msg).toContain('Uncaught exception from a previous keyword')
      expect(output.error.msg).toContain('inter-keyword exception')
    })
  })

  // ---------------------------------------------------------------------------
  // Uncaught errors after the last keyword
  // ---------------------------------------------------------------------------

  describe('uncaught errors after last keyword', () => {
    test('unhandled rejection after last keyword causes runner.close() to throw', async () => {
      const r = require('../api/runner/runner')()
      r.setThrowExceptionOnError(false)
      await r.run('FireAndForgetRejectionKW', {})
      // Wait for the rejection to fire inside the fork before closing.
      await new Promise(resolve => setTimeout(resolve, 100))
      await expect(r.close()).rejects.toThrow('inter-keyword rejection')
    })

    test('uncaught exception after last keyword causes runner.close() to throw', async () => {
      const r = require('../api/runner/runner')()
      r.setThrowExceptionOnError(false)
      await r.run('FireAndForgetExceptionKW', {})
      // Wait for the exception to fire inside the fork before closing.
      await new Promise(resolve => setTimeout(resolve, 100))
      await expect(r.close()).rejects.toThrow('inter-keyword exception')
    })
  })

  describe('syntax error', () => {
    let runner

    beforeEach(() => {
      runner = require('../api/runner/runner')({Property1: 'Prop1'}, {keywordDirectory: 'test/keywords-with-syntax-error'})
      runner.setThrowExceptionOnError(false)
    })

    afterEach(async () => {
      await runner.close()
    })

    test('keyword file with syntax error', async () => {
      const output = await runner.run('SyntaxErrorKW', {Param1: 'Val1'})
      expect(output.error.msg).toContain('Error while importing keyword module keywordsWithSyntaxError.js: Unexpected identifier \'syntax\'')
      expect(output.error.type).toBe('TECHNICAL')
    })
  })

  // ---------------------------------------------------------------------------
  // Unknown keyword
  // ---------------------------------------------------------------------------

  test('returns error for non-existing keyword', async () => {
    const output = await runner.run('Not existing Keyword', {})
    expect(output.error.msg).toBe("Unable to find Keyword 'Not existing Keyword'")
  })

  // ---------------------------------------------------------------------------
  // output.add
  // ---------------------------------------------------------------------------

  test('output.add builds payload incrementally', async () => {
    const output = await runner.run('AddKW', {})
    expect(output.payload.name).toBe('Alice')
    expect(output.payload.score).toBe(42)
    expect(output.payload.active).toBe(true)
  })

  // ---------------------------------------------------------------------------
  // output.appendError
  // ---------------------------------------------------------------------------

  describe('output.appendError', () => {
    test('appends to an existing error', async () => {
      const output = await runner.run('AppendErrorToExistingKW', {})
      expect(output.error.msg).toBe('base error + extra detail')
      expect(output.error.type).toBe('TECHNICAL')
    })

    test('creates a new error when none exists', async () => {
      const output = await runner.run('AppendErrorToNoneKW', {})
      expect(output.error.msg).toBe('fresh error')
      expect(output.error.type).toBe('TECHNICAL')
    })
  })

  // ---------------------------------------------------------------------------
  // output.attach
  // ---------------------------------------------------------------------------

  test('output.attach adds an attachment', async () => {
    const output = await runner.run('AttachKW', {})
    expect(output.attachments).toHaveLength(1)
    expect(output.attachments[0].name).toBe('report.txt')
    expect(output.attachments[0].isDirectory).toBe(false)
  })

  // ---------------------------------------------------------------------------
  // Measurement methods
  // ---------------------------------------------------------------------------

  describe('measurements', () => {
    test('startMeasure / stopMeasure produces a PASSED measure', async () => {
      const output = await runner.run('StartStopMeasureKW', {})
      expect(output.measures).toHaveLength(1)
      expect(output.measures[0].name).toBe('step1')
      expect(output.measures[0].status).toBe(MeasureStatus.PASSED)
      expect(output.measures[0].duration).toBeGreaterThanOrEqual(10)
      expect(typeof output.measures[0].begin).toBe('number')
    })

    test('stopMeasure accepts an explicit FAILED status and custom data', async () => {
      const output = await runner.run('StartStopMeasureWithStatusKW', {})
      expect(output.measures[0].name).toBe('failing-step')
      expect(output.measures[0].status).toBe(MeasureStatus.FAILED)
      expect(output.measures[0].data.reason).toBe('assertion failed')
    })

    test('addMeasure accepts a pre-set duration and TECHNICAL_ERROR status', async () => {
      const output = await runner.run('AddMeasureKW', {})
      expect(output.measures[0].name).toBe('pre-timed')
      expect(output.measures[0].duration).toBe(150)
      expect(output.measures[0].status).toBe(MeasureStatus.TECHNICAL_ERROR)
      expect(output.measures[0].data.info).toBe('test')
      expect(typeof output.measures[0].begin).toBe('number')
    })

    test('multiple measures accumulate in a single keyword call', async () => {
      const output = await runner.run('MultipleMeasuresKW', {})
      expect(output.measures).toHaveLength(3)
      expect(output.measures[0].name).toBe('first')
      expect(output.measures[0].status).toBe(MeasureStatus.PASSED)
      expect(output.measures[1].name).toBe('second')
      expect(output.measures[1].status).toBe(MeasureStatus.FAILED)
      expect(output.measures[2].name).toBe('third')
      expect(output.measures[2].duration).toBe(50)
    })
  })

  // ---------------------------------------------------------------------------
  // session
  // ---------------------------------------------------------------------------

  describe('session', () => {
    test('values stored via session.set() are accessible in a later keyword via session.get()', async () => {
      await runner.run('SessionSetKW', { value: 'hello' })
      const output = await runner.run('SessionGetKW')
      expect(output.payload.value).toBe('hello')
    })

    test('values stored via dot notation are accessible in a later keyword', async () => {
      await runner.run('SessionSetDotKW', { value: 'world' })
      const output = await runner.run('SessionGetDotKW')
      expect(output.payload.value).toBe('world')
    })

    test('session value is updated when set again', async () => {
      await runner.run('SessionSetKW', { value: 'first' })
      await runner.run('SessionSetKW', { value: 'second' })
      const output = await runner.run('SessionGetKW')
      expect(output.payload.value).toBe('second')
    })
  })

  // ---------------------------------------------------------------------------
  // session auto-disposal on runner.close()
  //
  // Keywords run in a forked subprocess whose own session holds user-stored
  // resources.  When runner.close() is called, the fork receives KILL and its
  // session disposes those resources.  The close() side-effect (writing a temp
  // file) is the observable signal crossing the process boundary.
  // ---------------------------------------------------------------------------

  describe('session auto-disposal on runner.close()', () => {
    const os = require('os')
    const path = require('path')
    const fs = require('fs')

    test('close() is called on a resource stored in the session when runner.close() is invoked', async () => {
      const closePath = path.join(os.tmpdir(), `step-session-close-${Date.now()}.txt`)
      const r = require('../api/runner/runner')()
      r.setThrowExceptionOnError(false)
      try {
        await r.run('StoreCloseableKW', { closePath })
        expect(fs.existsSync(closePath)).toBe(false) // resource not yet closed

        await r.close()

        expect(fs.existsSync(closePath)).toBe(true) // resource closed synchronously with the fork
      } finally {
        if (fs.existsSync(closePath)) fs.unlinkSync(closePath)
      }
    })
  })

  // ---------------------------------------------------------------------------
  // properties
  // ---------------------------------------------------------------------------

  describe('properties', () => {
    test('property passed to runner constructor is accessible in a keyword', async () => {
      const output = await runner.run('GetPropertyKW', { key: 'Property1' })
      expect(output.payload.value).toBe('Prop1')
    })

    test('all properties passed to runner constructor are accessible', async () => {
      const r = require('../api/runner/runner')({ KeyA: 'ValA', KeyB: 'ValB' })
      r.setThrowExceptionOnError(false)
      try {
        const outputA = await r.run('GetPropertyKW', { key: 'KeyA' })
        const outputB = await r.run('GetPropertyKW', { key: 'KeyB' })
        expect(outputA.payload.value).toBe('ValA')
        expect(outputB.payload.value).toBe('ValB')
      } finally {
        await r.close()
      }
    })

    test('runner without properties gives keywords an empty properties object', async () => {
      const r = require('../api/runner/runner')()
      r.setThrowExceptionOnError(false)
      try {
        const output = await r.run('GetPropertyKW', { key: 'anyKey' })
        expect(output.payload.value).toBeUndefined()
        expect(output.error).toBeUndefined()
      } finally {
        await r.close()
      }
    })
  })

  // ---------------------------------------------------------------------------
  // beforeKeyword and afterKeyword hooks
  // ---------------------------------------------------------------------------

  describe('beforeKeyword and afterKeyword hooks', () => {
    beforeEach(async () => {
      await runner.run('GetHookCallsKW')
    })

    test('beforeKeyword is called with the keyword name before execution', async () => {
      await runner.run('Echo', {})
      const { payload: { calls } } = await runner.run('GetHookCallsKW')
      expect(calls).toContain('before:Echo')
    })

    test('afterKeyword is called with the keyword name after successful execution', async () => {
      await runner.run('Echo', {})
      const { payload: { calls } } = await runner.run('GetHookCallsKW')
      expect(calls).toContain('after:Echo')
    })

    test('beforeKeyword is called before afterKeyword', async () => {
      await runner.run('Echo', {})
      const { payload: { calls } } = await runner.run('GetHookCallsKW')
      expect(calls.indexOf('before:Echo')).toBeLessThan(calls.indexOf('after:Echo'))
    })

    test('afterKeyword is called even when the keyword throws', async () => {
      await runner.run('ErrorTestKW', { ErrorMsg: 'test error', rethrow_error: false })
      const { payload: { calls } } = await runner.run('GetHookCallsKW')
      expect(calls).toContain('after:ErrorTestKW')
    })
  })
})

// ---------------------------------------------------------------------------
// MeasureStatus validation (no runner needed)
// ---------------------------------------------------------------------------

describe('MeasureStatus validation', () => {
  test('stopMeasure throws TypeError for unknown status', () => {
    const ob = new OutputBuilder(null)
    ob.startMeasure('test')
    expect(() => ob.stopMeasure({ status: 'INVALID_STATUS' })).toThrow(TypeError)
    expect(() => ob.stopMeasure({ status: 'INVALID_STATUS' })).toThrow('INVALID_STATUS')
  })

  test('addMeasure throws TypeError for unknown status', () => {
    const ob = new OutputBuilder(null)
    expect(() => ob.addMeasure('test', 100, { status: 'WRONG' })).toThrow(TypeError)
    expect(() => ob.addMeasure('test', 100, { status: 'WRONG' })).toThrow('WRONG')
  })

  test('all valid MeasureStatus values are accepted without throwing', () => {
    const ob = new OutputBuilder(null)
    for (const status of Object.values(MeasureStatus)) {
      ob.startMeasure('check')
      expect(() => ob.stopMeasure({ status })).not.toThrow()
    }
  })
})
