const fs = require('fs')
const os = require('os')
const path = require('path')
const http = require('http')
const FileManager = require('../api/filemanager/filemanager')

// Returns a port number that was just released, so the first connection attempt
// to it gets ECONNREFUSED until a server is (re)started on it.
function getFreePort() {
  return new Promise((resolve) => {
    const srv = http.createServer()
    srv.listen(0, '127.0.0.1', () => {
      const port = srv.address().port
      srv.close(() => resolve(port))
    })
  })
}

function startFileServer(port, body) {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      res.writeHead(200, { 'content-disposition': 'attachment; filename = keyword.txt;' })
      res.end(body)
    })
    server.listen(port, '127.0.0.1', () => resolve(server))
  })
}

function newFileManager(overrides = {}) {
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'fm-retry-'))
  const agentContext = {
    gridSecurity: undefined,
    filemanagerPath: tmpDir,
    gridMaxRetries: 5,
    gridRetryDelayMs: 50,
    ...overrides,
  }
  return new FileManager(agentContext)
}

describe('FileManager connection retry', () => {
  test('retries a refused connection and eventually downloads the file', async () => {
    const port = await getFreePort()
    const fileManager = newFileManager()
    const controllerUrl = `http://127.0.0.1:${port}/grid/file/`

    // Nothing is listening yet -> the first attempts get ECONNREFUSED.
    const downloadPromise = fileManager.loadOrGetKeywordFile(controllerUrl, 'fileA', 'v1')

    // Bring the server up after a couple of failed attempts (grace period is 50ms).
    await new Promise((r) => setTimeout(r, 130))
    const server = await startFileServer(port, 'hello world')

    try {
      const filePath = await downloadPromise
      expect(fs.readFileSync(filePath, 'utf8')).toBe('hello world')
    } finally {
      server.close()
    }
  })

  test('rejects after exhausting the retries when the connection keeps being refused', async () => {
    const port = await getFreePort()
    const fileManager = newFileManager({ gridMaxRetries: 2, gridRetryDelayMs: 20 })
    const controllerUrl = `http://127.0.0.1:${port}/grid/file/`

    await expect(fileManager.loadOrGetKeywordFile(controllerUrl, 'fileB', 'v1'))
      .rejects.toMatchObject({ code: 'ECONNREFUSED' })
  })

  test('does not retry a non-200 server response', async () => {
    let hits = 0
    const server = await new Promise((resolve) => {
      const s = http.createServer((req, res) => {
        hits++
        res.writeHead(500)
        res.end('boom')
      })
      s.listen(0, '127.0.0.1', () => resolve(s))
    })
    const port = server.address().port
    const fileManager = newFileManager({ gridMaxRetries: 3, gridRetryDelayMs: 20 })

    try {
      await expect(fileManager.loadOrGetKeywordFile(`http://127.0.0.1:${port}/grid/file/`, 'fileC', 'v1'))
        .rejects.toThrow(/HTTP status 500/)
      // The server-side error must not be retried: a single request is expected.
      expect(hits).toBe(1)
    } finally {
      server.close()
    }
  })

  test('retries read timeouts and rejects with ETIMEDOUT when the server never responds', async () => {
    let hits = 0
    const server = await new Promise((resolve) => {
      // Accept the connection but never send a response, to trigger a read timeout.
      const s = http.createServer(() => { hits++ })
      s.listen(0, '127.0.0.1', () => resolve(s))
    })
    const port = server.address().port
    const fileManager = newFileManager({ gridMaxRetries: 2, gridRetryDelayMs: 20, gridConnectTimeout: 100, gridReadTimeout: 100 })

    try {
      await expect(fileManager.loadOrGetKeywordFile(`http://127.0.0.1:${port}/grid/file/`, 'fileD', 'v1'))
        .rejects.toMatchObject({ code: 'ETIMEDOUT' })
      // Initial attempt + 2 retries.
      expect(hits).toBe(3)
    } finally {
      server.close()
    }
  })
})
