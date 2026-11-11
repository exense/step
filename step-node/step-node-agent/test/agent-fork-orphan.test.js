const { fork, spawn } = require('child_process')
const fs = require('fs')
const os = require('os')
const path = require('path')

// SED-5045: when the agent goes away while a keyword runs, the fork's stdout/stderr pipes break.
// The fork used to log the resulting EPIPE through the same broken pipe from its uncaughtException
// handler, raising a new EPIPE each time: an endless loop that outlived the agent and, for keywords
// copying their output to a file, filled the disk.
//
// Every test here bounds what it can write: the keyword's tee stops at TEE_MAX_BYTES, and any
// process left behind is killed and the temporary directory removed in afterEach.

const AGENT_ROOT = path.resolve(__dirname, '..')
const AGENT_FORK = path.join(AGENT_ROOT, 'api', 'controllers', 'agent-fork.js')
const AGENT_STAND_IN = path.join(__dirname, 'fixtures', 'agent-stand-in.js')
const TEE_MAX_BYTES = 1024 * 1024
// Generous upper bound for what the fork may legitimately write once its pipes are broken: a few
// log lines, far below what the EPIPE loop produces (hundreds of KB per second).
const MAX_BYTES_AFTER_FAILURE = 16 * 1024

// On Windows the fork is terminated by the OS together with the agent (a non-detached child does
// not outlive its parent there), so none of its handlers run: only POSIX lets it outlive the agent,
// which is where the bug occurs and where releasing the session on disconnect can be observed.
const posixOnlyTest = process.platform === 'win32' ? test.skip : test

const sleep = (ms) => new Promise(r => setTimeout(r, ms))

function isAlive(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}

async function waitFor(condition, timeoutMs, intervalMs = 50) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (condition()) return true
    await sleep(intervalMs)
  }
  return condition()
}

describe('agent fork when its agent goes away (SED-5045)', () => {
  let tmpDir
  let teePath
  const pidsToKill = []

  const teeSize = () => {
    try {
      return fs.statSync(teePath).size
    } catch {
      return 0
    }
  }

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sed-5045-'))
    teePath = path.join(tmpDir, 'keyword-output.log')
  })

  afterEach(async () => {
    for (const pid of pidsToKill.splice(0)) {
      if (isAlive(pid)) {
        try { process.kill(pid, 'SIGKILL') } catch { /* already gone */ }
      }
    }
    await sleep(100)
    fs.rmSync(tmpDir, { recursive: true, force: true })
  })

  const keywordInput = (durationMs) => ({ durationMs, intervalMs: 20, teePath, teeMaxBytes: TEE_MAX_BYTES })

  // Starts the agent stand-in running LongRunningLoggingKW, waits until the keyword writes, and
  // returns the stand-in process and the fork's pid. `env` is passed on to the fork.
  async function startKeywordInAgentStandIn(extraInput = {}, env = {}) {
    const input = { ...keywordInput(30000), ...extraInput }
    const standIn = spawn(process.execPath, [AGENT_STAND_IN, AGENT_ROOT, 'LongRunningLoggingKW', JSON.stringify(input)],
      { stdio: ['ignore', 'pipe', 'inherit'], env: { ...process.env, ...env } })
    pidsToKill.push(standIn.pid)

    const forkPid = await new Promise((resolve, reject) => {
      standIn.stdout.once('data', (d) => resolve(JSON.parse(d.toString()).forkPid))
      standIn.once('error', reject)
    })
    pidsToKill.push(forkPid)

    // Wait until the keyword is actually running and writing
    expect(await waitFor(() => teeSize() > 0, 10000)).toBe(true)
    return { standIn, forkPid }
  }

  test('fork exits when the agent is killed while a keyword is running', async () => {
    const { standIn, forkPid } = await startKeywordInAgentStandIn()

    standIn.kill('SIGKILL')
    const sizeAtKill = teeSize()

    expect(await waitFor(() => !isAlive(forkPid), 3000)).toBe(true)
    expect(teeSize() - sizeAtKill).toBeLessThan(MAX_BYTES_AFTER_FAILURE)
  }, 20000)

  posixOnlyTest('fork releases its session before exiting when the agent is killed', async () => {
    const closePath = path.join(tmpDir, 'closed.txt')
    const { standIn, forkPid } = await startKeywordInAgentStandIn({ closePath })

    standIn.kill('SIGKILL')

    expect(await waitFor(() => !isAlive(forkPid), 3000)).toBe(true)
    expect(fs.existsSync(closePath)).toBe(true)
  }, 20000)

  posixOnlyTest('fork still exits when releasing its session hangs after the agent is killed', async () => {
    const { standIn, forkPid } = await startKeywordInAgentStandIn({ hangingClose: true },
      { STEP_AGENT_FORK_SHUTDOWN_TIMEOUT_MS: '1000' })

    standIn.kill('SIGKILL')

    // Still releasing within the timeout, gone once it has elapsed
    await sleep(500)
    expect(isAlive(forkPid)).toBe(true)
    expect(await waitFor(() => !isAlive(forkPid), 3000)).toBe(true)
  }, 20000)

  test('fork does not loop on a broken stdout/stderr and still returns the keyword result', async () => {
    const child = fork(AGENT_FORK, [], { cwd: AGENT_ROOT, silent: true })
    pidsToKill.push(child.pid)
    child.stdout.on('data', () => {})
    child.stderr.on('data', () => {})

    const result = new Promise((resolve, reject) => {
      child.once('message', resolve)
      child.once('error', reject)
    })
    child.send({ type: 'KEYWORD', projectPath: AGENT_ROOT, functionName: 'LongRunningLoggingKW', input: keywordInput(1500), properties: {}, keywordDirectory: 'test/keywords' })

    expect(await waitFor(() => teeSize() > 0, 10000)).toBe(true)

    // Close our read ends of the fork's output pipes while keeping the IPC channel: the fork's next
    // writes fail with EPIPE, as when the agent dies, but it can still report back to us.
    child.stdout.destroy()
    child.stderr.destroy()
    const sizeAtBreak = teeSize()

    const output = await result
    // The EPIPE is handled by the fork; it must not surface as an uncaught exception of the keyword
    expect(output.payload.error).toBeUndefined()
    expect(teeSize() - sizeAtBreak).toBeLessThan(MAX_BYTES_AFTER_FAILURE)

    const exit = new Promise((resolve) => child.once('exit', (code) => resolve(code)))
    child.send({ type: 'KILL' })
    expect(await exit).toBe(0)
  }, 20000)
})
