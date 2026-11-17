const path = require('path')
const { ForkedAgent } = require('../api/controllers/agent')

// A keyword must be able to see when its call times out, so that a long-running keyword can stop on
// its own before the agent gives up on it, and still report its output, measures and attachments.

const AGENT_ROOT = path.resolve(__dirname, '..')
const KEYWORD_DIRECTORY = 'test/keywords'

describe('keyword call deadline', () => {
  let forkedAgent

  beforeEach(() => {
    forkedAgent = new ForkedAgent(AGENT_ROOT)
  })

  afterEach(async () => {
    await forkedAgent.close()
  })

  function run(functionName, input, deadline, properties = {}) {
    return forkedAgent.runKeywordTask(AGENT_ROOT, functionName, input, properties, deadline, false, KEYWORD_DIRECTORY)
  }

  test('the deadline is passed to the keyword as the $keywordDeadline property', async () => {
    const deadline = Date.now() + 10000
    const properties = { myProp: 'value' }

    const { result } = await run('Echo', {}, deadline, properties)

    expect(result.payload.payload.properties).toEqual({ myProp: 'value', $keywordDeadline: String(deadline) })
    // The caller's properties are left untouched
    expect(properties).toEqual({ myProp: 'value' })
  }, 15000)

  test('no $keywordDeadline property is set when the call has no deadline', async () => {
    const { result } = await run('Echo', {}, undefined, { myProp: 'value' })

    expect(result.payload.payload.properties).toEqual({ myProp: 'value' })
  }, 15000)

  test('a keyword that stops before its deadline returns its output instead of timing out', async () => {
    const deadline = Date.now() + 1500

    const { result } = await run('StopBeforeDeadlineKW', { marginMs: 300 }, deadline)

    expect(result.payload.payload.stoppedBeforeDeadline).toBe(true)
    expect(result.payload.payload.iterations).toBeGreaterThan(0)
    expect(Date.now()).toBeLessThan(deadline)
  }, 15000)

  test('a keyword still running at its deadline times out at that deadline', async () => {
    const deadline = Date.now() + 500

    await expect(run('HangKW', {}, deadline)).rejects.toThrow(/^Keyword execution timed out after \d+ms: the call timeout expired at /)
    expect(Date.now()).toBeGreaterThanOrEqual(deadline)
    expect(Date.now()).toBeLessThan(deadline + 1000)
  }, 15000)

  test('a deadline already passed times out immediately', async () => {
    await expect(run('HangKW', {}, Date.now() - 1000)).rejects.toThrow('Keyword execution timed out')
  }, 15000)
})
