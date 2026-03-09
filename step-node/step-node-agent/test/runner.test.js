const { OutputBuilder, MeasureStatus } = require('../api/controllers/output')

describe('runner', () => {
  let runner

  beforeAll(() => {
    runner = require('../api/runner/runner')({ Property1: 'Prop1' })
  })

  afterAll(() => {
    runner.close()
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
      expect(output.attachments).toHaveLength(1)
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

  test('rejected promises do not surface as output error', async () => {
    const output = await runner.run('ErrorRejectedPromiseTestKW', { Param1: 'Val1' })
    expect(output.error).toBeUndefined()
  })

  test('uncaught exceptions do not surface as output error', async () => {
    const output = await runner.run('ErrorUncaughtExceptionTestKW', { Param1: 'Val1' })
    expect(output.error).toBeUndefined()
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
