const { fork } = require('child_process')
const path = require('path')

// The KILL handler of the fork must always reach process.exit(), even when the session
// disposal fails. If it doesn't, the fork stays alive and the parent's close() hangs
// forever waiting for the 'exit' event.

const AGENT_ROOT = path.resolve(__dirname, '..')
const AGENT_FORK = path.join(AGENT_ROOT, 'api', 'controllers', 'agent-fork.js')

describe('agent fork KILL handling', () => {
  let child

  beforeEach(() => {
    child = fork(AGENT_FORK, [], { cwd: AGENT_ROOT, silent: true })
  })

  afterEach(() => {
    if (child && child.exitCode === null && child.signalCode === null) {
      child.kill()
    }
  })

  function runKeyword(functionName, input = {}) {
    return new Promise((resolve, reject) => {
      const onMessage = (msg) => {
        child.removeListener('message', onMessage)
        resolve(msg)
      }
      child.on('message', onMessage)
      child.once('error', reject)
      child.send({ type: 'KEYWORD', projectPath: AGENT_ROOT, functionName, input, properties: {}, keywordDirectory: 'test/keywords' })
    })
  }

  // Sends KILL and resolves with the exit code and all messages received before the exit
  function kill() {
    return new Promise((resolve, reject) => {
      const messages = []
      child.on('message', (msg) => messages.push(msg))
      child.once('error', reject)
      child.once('exit', (code, signal) => resolve({ code, signal, messages }))
      child.send({ type: 'KILL' })
    })
  }

  test('fork exits when session disposal fails and reports the failure to the parent', async () => {
    await runKeyword('StoreFailingCloseableKW', { errorMsg: 'disposal boom' })

    const { code, signal, messages } = await kill()

    expect(signal).toBeNull()
    expect(code).toBe(0)
    const closeResult = messages.find(m => m && m.type === 'CLOSE_RESULT')
    expect(closeResult).toBeDefined()
    expect(closeResult.errors.map(e => e.message).join('; ')).toContain('disposal boom')
  }, 10000)

  test('errors that occurred after the last keyword are still reported when disposal fails', async () => {
    await runKeyword('StoreFailingCloseableKW', { errorMsg: 'disposal boom' })
    await runKeyword('FireAndForgetRejectionKW', {})
    // Wait for the rejection to fire inside the fork (setTimeout 50 ms + margin).
    await new Promise(r => setTimeout(r, 100))

    const { code, messages } = await kill()

    expect(code).toBe(0)
    const closeResult = messages.find(m => m && m.type === 'CLOSE_RESULT')
    const allMessages = closeResult.errors.map(e => e.message).join('; ')
    expect(allMessages).toContain('inter-keyword rejection')
    expect(allMessages).toContain('disposal boom')
  }, 10000)

  test('fork exits with no CLOSE_RESULT when session disposal succeeds', async () => {
    await runKeyword('SessionSetKW', { value: 'hello' })

    const { code, messages } = await kill()

    expect(code).toBe(0)
    expect(messages.find(m => m && m.type === 'CLOSE_RESULT')).toBeUndefined()
  }, 10000)
})
