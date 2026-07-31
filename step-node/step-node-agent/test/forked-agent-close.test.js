const { EventEmitter } = require('events')
const fs = require('fs')
const os = require('os')
const path = require('path')
const { ForkedAgent } = require('../api/controllers/agent')

// close() must always return, even when the forked process never exits on its own: a stuck fork
// would otherwise block the release of its token indefinitely.

// Stands in for the ChildProcess of a forked agent. `exitsOn` lists the events that make it exit:
// 'KILL' (the KILL message), 'SIGTERM' and/or 'SIGKILL'. A fork listed for none of them never exits.
class FakeForkProcess extends EventEmitter {
  constructor(exitsOn = []) {
    super()
    this.exitCode = null
    this.signalCode = null
    this.sentMessages = []
    this.receivedSignals = []
    this.exitsOn = exitsOn
  }

  send(message) {
    this.sentMessages.push(message)
    if (message.type === 'KILL' && this.exitsOn.includes('KILL')) {
      setImmediate(() => this.exitWith(0, null))
    }
  }

  kill(signal = 'SIGTERM') {
    this.receivedSignals.push(signal)
    if (this.exitsOn.includes(signal)) {
      setImmediate(() => this.exitWith(null, signal))
    }
    return true
  }

  exitWith(code, signal) {
    this.exitCode = code
    this.signalCode = signal
    this.emit('exit', code, signal)
  }
}

function forkedAgentFor(forkProcess, shutdownTimeoutMs = 50, terminationGracePeriodMs = 50) {
  const forkedAgent = Object.create(ForkedAgent.prototype)
  forkedAgent.forkProcess = forkProcess
  forkedAgent.shutdownTimeoutMs = shutdownTimeoutMs
  forkedAgent.terminationGracePeriodMs = terminationGracePeriodMs
  // rm is called with force: true, so a non-existing directory is a no-op. The path is nested
  // so that removing its parent is a no-op too, rather than targeting the temp directory itself.
  forkedAgent.agentForkerLibPath = path.join(os.tmpdir(), 'step-agent-fork-libs-that-does-not-exist', 'fork-test')
  return forkedAgent
}

describe('ForkedAgent.close()', () => {

  test('resolves without signalling the fork when it exits on the KILL message', async () => {
    const forkProcess = new FakeForkProcess(['KILL'])

    await expect(forkedAgentFor(forkProcess).close()).resolves.toBeUndefined()

    expect(forkProcess.sentMessages).toEqual([{ type: 'KILL' }])
    expect(forkProcess.receivedSignals).toEqual([])
  })

  test('reports the close errors sent by the fork before its exit', async () => {
    const forkProcess = new FakeForkProcess()
    forkProcess.send = function (message) {
      this.sentMessages.push(message)
      setImmediate(() => {
        this.emit('message', { type: 'CLOSE_RESULT', errors: [{ message: 'disposal boom' }] })
        this.exitWith(0, null)
      })
    }

    await expect(forkedAgentFor(forkProcess).close()).rejects.toThrow('disposal boom')
  })

  test('terminates the fork with SIGTERM and reports the timeout when it does not exit in time', async () => {
    const forkProcess = new FakeForkProcess(['SIGTERM'])

    await expect(forkedAgentFor(forkProcess).close()).rejects.toThrow('did not exit within 50ms')

    expect(forkProcess.receivedSignals).toEqual(['SIGTERM'])
  })

  test('escalates to SIGKILL when the fork ignores SIGTERM', async () => {
    const forkProcess = new FakeForkProcess(['SIGKILL'])

    await expect(forkedAgentFor(forkProcess).close()).rejects.toThrow('had to be terminated')

    expect(forkProcess.receivedSignals).toEqual(['SIGTERM', 'SIGKILL'])
  })

  test('returns instead of hanging when the fork survives SIGKILL', async () => {
    const forkProcess = new FakeForkProcess()

    await expect(forkedAgentFor(forkProcess).close()).rejects.toThrow('had to be terminated')

    expect(forkProcess.receivedSignals).toEqual(['SIGTERM', 'SIGKILL'])
    expect(forkProcess.exitCode).toBeNull()
  })

  test('resolves immediately when the fork has already exited', async () => {
    const forkProcess = new FakeForkProcess()
    forkProcess.exitWith(0, null)

    await expect(forkedAgentFor(forkProcess).close()).resolves.toBeUndefined()

    expect(forkProcess.receivedSignals).toEqual([])
  })
})

// Forks of the same keyword project must not share their lib directory: close() deletes it, and
// local runs as well as a re-created session put several forks in the same project directory.
describe('ForkedAgent lib directory isolation', () => {
  let keywordProjectPath
  const forkedAgents = []

  beforeEach(() => {
    keywordProjectPath = fs.mkdtempSync(path.join(os.tmpdir(), 'step-keyword-project-'))
  })

  afterEach(async () => {
    await Promise.all(forkedAgents.splice(0).map(a => a.close().catch(() => {})))
    fs.rmSync(keywordProjectPath, { recursive: true, force: true })
  })

  function startForkedAgent() {
    const forkedAgent = new ForkedAgent(keywordProjectPath)
    forkedAgents.push(forkedAgent)
    return forkedAgent
  }

  test('two forks of the same keyword project get distinct lib directories', () => {
    const first = startForkedAgent()
    const second = startForkedAgent()

    expect(first.agentForkerLibPath).not.toEqual(second.agentForkerLibPath)
    expect(path.dirname(first.agentForkerLibPath)).toEqual(path.join(keywordProjectPath, 'agent-fork-libs'))
    for (const forkedAgent of [first, second]) {
      expect(fs.existsSync(path.join(forkedAgent.agentForkerLibPath, 'agent-fork.js'))).toBe(true)
      expect(fs.existsSync(path.join(forkedAgent.agentForkerLibPath, 'live-reporting'))).toBe(true)
    }
  }, 15000)

  test('closing one fork leaves the libs of the other fork intact', async () => {
    const first = startForkedAgent()
    const second = startForkedAgent()

    await first.close()

    expect(fs.existsSync(first.agentForkerLibPath)).toBe(false)
    expect(fs.existsSync(path.join(second.agentForkerLibPath, 'agent-fork.js'))).toBe(true)
  }, 15000)

  // Sending to a dead fork does not throw: it reports the failure asynchronously, and an
  // unhandled 'error' event on the child process would bring the whole agent down.
  test('closing a fork that already died reports no error and keeps the agent alive', async () => {
    const forkedAgent = startForkedAgent()
    forkedAgent.forkProcess.kill('SIGKILL')
    await new Promise(resolve => forkedAgent.forkProcess.once('exit', resolve))

    await expect(forkedAgent.close()).resolves.toBeUndefined()
    expect(fs.existsSync(forkedAgent.agentForkerLibPath)).toBe(false)
  }, 15000)

  test('the shared parent directory is removed once the last fork is closed', async () => {
    const first = startForkedAgent()
    const second = startForkedAgent()
    const libsRoot = path.join(keywordProjectPath, 'agent-fork-libs')

    await first.close()
    expect(fs.existsSync(libsRoot)).toBe(true)

    await second.close()
    expect(fs.existsSync(libsRoot)).toBe(false)
  }, 15000)
})