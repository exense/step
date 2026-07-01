const crypto = require('crypto')
const fs = require('fs')
const os = require('os')
const path = require('path')
const { WebSocketServer } = require('ws')
const {
  createLiveReporting,
  StreamingUploads,
  QuotaExceededError,
} = require('../api/controllers/live-reporting')

/**
 * Mock streaming upload server implementing the server side of the protocol.
 * Per connection it records the metadata, the received binary bytes, and the FinishUpload checksum,
 * then replies with UploadAcknowledged. Behavior can be tweaked per test via `options`.
 */
function startMockUploadServer(options = {}) {
  const connections = []
  const wss = new WebSocketServer({ port: 0 })
  wss.on('connection', (socket, req) => {
    const conn = { url: req.url, binary: Buffer.alloc(0), metadata: null, clientChecksum: null }
    connections.push(conn)
    socket.on('message', (data, isBinary) => {
      if (isBinary) {
        if (options.rejectWithQuotaOnData) {
          socket.close(1008, 'QuotaExceededException: storage full')
          return
        }
        conn.binary = Buffer.concat([conn.binary, data])
        return
      }
      const msg = JSON.parse(data.toString())
      if (msg['@'] === 'StartUpload') {
        conn.metadata = msg.metadata
        if (options.rejectWithQuota) {
          socket.close(1008, 'QuotaExceededException: file too large')
          return
        }
        socket.send(JSON.stringify({ '@': 'ReadyForUpload', reference: { uri: options.referenceUri || 'streaming://resource/abc' } }))
      } else if (msg['@'] === 'FinishUpload') {
        conn.clientChecksum = msg.checksum
        const serverChecksum = options.corruptChecksum
          ? 'deadbeef'
          : crypto.createHash('md5').update(conn.binary).digest('hex')
        socket.send(JSON.stringify({
          '@': 'UploadAcknowledged',
          size: conn.binary.length,
          numberOfLines: options.numberOfLines != null ? options.numberOfLines : null,
          checksum: serverChecksum,
        }))
      }
    })
  })
  return new Promise((resolve) => {
    wss.on('listening', () => resolve({ wss, port: wss.address().port, connections }))
  })
}

function uploadProps(port, overrides = {}) {
  return {
    streamingUploadContextId: overrides.contextId || 'upctx',
    '$liveReporting.streaming.websocket.upload.path': overrides.path || 'ws/streaming/upload',
    'step.reporting.url': `http://localhost:${port}`,
    ...overrides.extra,
  }
}

function writeTempFile(content) {
  const file = path.join(os.tmpdir(), `step-upload-${Date.now()}-${Math.random().toString(36).slice(2)}.txt`)
  fs.writeFileSync(file, content)
  return file
}

describe('live reporting - file uploads wiring', () => {
  test('no streaming context => discarding uploads, reports DISABLED with file size', async () => {
    const lr = createLiveReporting({})
    expect(lr.fileUploads).toBeInstanceOf(StreamingUploads)
    const file = writeTempFile('hello world')
    try {
      const status = await lr.fileUploads.uploadBinaryFile(file)
      expect(status.transferStatus).toBe('DISABLED')
      expect(status.size).toBe(Buffer.byteLength('hello world'))
      expect(status.reference).toBeNull()
    } finally {
      fs.unlinkSync(file)
      await lr.close()
    }
  })
})

describe('live reporting - WebSocket upload', () => {
  test('uploads a binary file and resolves with COMPLETED status and reference', async () => {
    const { wss, port, connections } = await startMockUploadServer({ referenceUri: 'streaming://res/42' })
    const file = writeTempFile('the quick brown fox')
    const lr = createLiveReporting(uploadProps(port))
    try {
      const status = await lr.fileUploads.uploadBinaryFile(file, { mimeType: 'application/octet-stream' })
      expect(status.transferStatus).toBe('COMPLETED')
      expect(status.size).toBe(Buffer.byteLength('the quick brown fox'))
      expect(status.reference).toEqual({ uri: 'streaming://res/42' })

      expect(connections).toHaveLength(1)
      expect(connections[0].url).toBe('/ws/streaming/upload?streamingUploadContextId=upctx')
      expect(connections[0].metadata).toEqual({
        filename: path.basename(file),
        mimeType: 'application/octet-stream',
        supportsLineAccess: false,
      })
      expect(connections[0].binary.toString()).toBe('the quick brown fox')
      // client checksum equals md5 of the bytes
      expect(connections[0].clientChecksum).toBe(crypto.createHash('md5').update('the quick brown fox').digest('hex'))
    } finally {
      fs.unlinkSync(file)
      await lr.close()
      wss.close()
    }
  })

  test('uploads a file larger than the read-chunk size in multiple fragments', async () => {
    const { wss, port, connections } = await startMockUploadServer()
    // ~200 KB > MAX_READ_CHUNK_BYTES (64 KB): forces several read/send iterations.
    const content = Buffer.alloc(200 * 1024, 0x61) // 'a' repeated
    const file = path.join(os.tmpdir(), `step-big-${Date.now()}.bin`)
    fs.writeFileSync(file, content)
    const lr = createLiveReporting(uploadProps(port))
    try {
      const status = await lr.fileUploads.uploadBinaryFile(file)
      expect(status.transferStatus).toBe('COMPLETED')
      expect(status.size).toBe(content.length)
      expect(connections[0].binary.length).toBe(content.length)
      expect(connections[0].binary.equals(content)).toBe(true)
      expect(connections[0].clientChecksum).toBe(crypto.createHash('md5').update(content).digest('hex'))
    } finally {
      fs.unlinkSync(file)
      await lr.close()
      wss.close()
    }
  })

  test('uploadTextFile sets supportsLineAccess and text/plain by default', async () => {
    const { wss, port, connections } = await startMockUploadServer({ numberOfLines: 3 })
    const file = writeTempFile('a\nb\nc')
    const lr = createLiveReporting(uploadProps(port))
    try {
      const status = await lr.fileUploads.uploadTextFile(file)
      expect(status.transferStatus).toBe('COMPLETED')
      expect(status.numberOfLines).toBe(3)
      expect(connections[0].metadata.mimeType).toBe('text/plain')
      expect(connections[0].metadata.supportsLineAccess).toBe(true)
    } finally {
      fs.unlinkSync(file)
      await lr.close()
      wss.close()
    }
  })

  test('rejects with QuotaExceededError when the server closes with a quota reason', async () => {
    const { wss, port } = await startMockUploadServer({ rejectWithQuota: true })
    const file = writeTempFile('too big')
    const lr = createLiveReporting(uploadProps(port))
    try {
      await expect(lr.fileUploads.uploadBinaryFile(file)).rejects.toBeInstanceOf(QuotaExceededError)
    } finally {
      fs.unlinkSync(file)
      await lr.close()
      wss.close()
    }
  })

  test('rejects with QuotaExceededError when the server enforces quota mid-stream', async () => {
    const { wss, port } = await startMockUploadServer({ rejectWithQuotaOnData: true })
    const file = writeTempFile('some content that triggers a quota close while streaming')
    const lr = createLiveReporting(uploadProps(port))
    try {
      await expect(lr.fileUploads.uploadBinaryFile(file)).rejects.toBeInstanceOf(QuotaExceededError)
    } finally {
      fs.unlinkSync(file)
      await lr.close()
      wss.close()
    }
  })

  test('rejects on checksum mismatch', async () => {
    const { wss, port } = await startMockUploadServer({ corruptChecksum: true })
    const file = writeTempFile('content')
    const lr = createLiveReporting(uploadProps(port))
    try {
      await expect(lr.fileUploads.uploadBinaryFile(file)).rejects.toThrow('Checksum mismatch')
    } finally {
      fs.unlinkSync(file)
      await lr.close()
      wss.close()
    }
  })
})

describe('live reporting - streaming a growing file', () => {
  test('streams bytes appended after the upload starts and finishes on complete()', async () => {
    const { wss, port, connections } = await startMockUploadServer()
    const file = writeTempFile('') // start empty
    const lr = createLiveReporting(uploadProps(port))
    try {
      const upload = lr.fileUploads.startTextFileUpload(file)
      // Append data in chunks *after* the upload has started, with delays spanning poll intervals.
      const chunks = ['line-1\n', 'line-2\n', 'line-3\n']
      for (const chunk of chunks) {
        await new Promise((r) => setTimeout(r, 60))
        fs.appendFileSync(file, chunk)
      }
      await new Promise((r) => setTimeout(r, 60))
      const status = await upload.complete()

      const expected = chunks.join('')
      expect(status.transferStatus).toBe('COMPLETED')
      expect(status.size).toBe(Buffer.byteLength(expected))
      expect(connections).toHaveLength(1)
      // All appended bytes were received, reassembled in order from the fragmented frames.
      expect(connections[0].binary.toString()).toBe(expected)
      expect(connections[0].clientChecksum).toBe(crypto.createHash('md5').update(expected).digest('hex'))
    } finally {
      fs.unlinkSync(file)
      await lr.close()
      wss.close()
    }
  })

  test('reference is available after the server acknowledges the upload start', async () => {
    const { wss, port } = await startMockUploadServer({ referenceUri: 'streaming://res/live' })
    const file = writeTempFile('')
    const lr = createLiveReporting(uploadProps(port))
    try {
      const upload = lr.fileUploads.startBinaryFileUpload(file)
      fs.appendFileSync(file, 'data')
      const status = await upload.complete()
      expect(upload.reference).toEqual({ uri: 'streaming://res/live' })
      expect(status.reference).toEqual({ uri: 'streaming://res/live' })
    } finally {
      fs.unlinkSync(file)
      await lr.close()
      wss.close()
    }
  })

  test('close() cancels an upload the keyword started but never completed', async () => {
    const { wss, port } = await startMockUploadServer()
    const file = writeTempFile('partial')
    const lr = createLiveReporting(uploadProps(port))
    const upload = lr.fileUploads.startBinaryFileUpload(file)
    // Intentionally never call complete(). close() must cancel it and resolve without hanging.
    try {
      await expect(lr.close()).resolves.toBeUndefined()
      await expect(upload.complete()).rejects.toThrow('closed before the upload was completed')
    } finally {
      fs.unlinkSync(file)
      wss.close()
    }
  })
})

describe('live reporting - missing / unreadable source file', () => {
  test('completing an upload whose file never appeared uploads an empty resource', async () => {
    const { wss, port, connections } = await startMockUploadServer()
    // A path that is never created.
    const missing = path.join(os.tmpdir(), `step-never-${Date.now()}-${Math.random().toString(36).slice(2)}.txt`)
    const lr = createLiveReporting(uploadProps(port))
    try {
      const status = await lr.fileUploads.uploadBinaryFile(missing)
      expect(status.transferStatus).toBe('COMPLETED')
      expect(status.size).toBe(0)
      expect(connections).toHaveLength(1)
      expect(connections[0].binary.length).toBe(0)
    } finally {
      await lr.close()
      wss.close()
    }
  })

  test('fails fast when the file cannot be opened for a reason other than not-yet-created', async () => {
    const { wss, port } = await startMockUploadServer()
    // Simulate a file that exists but is not readable (e.g. permission denied). ENOENT means
    // "not created yet" and keeps polling; any other error must surface immediately.
    const openSpy = jest
      .spyOn(fs.promises, 'open')
      .mockRejectedValue(Object.assign(new Error('permission denied'), { code: 'EACCES' }))
    const lr = createLiveReporting(uploadProps(port))
    try {
      await expect(lr.fileUploads.uploadBinaryFile('/some/unreadable/file')).rejects.toThrow('permission denied')
    } finally {
      openSpy.mockRestore()
      await lr.close()
      wss.close()
    }
  })
})

describe('live reporting - file upload end to end through the forked agent', () => {
  test('a keyword uploads a file that reaches the controller', async () => {
    const { wss, port, connections } = await startMockUploadServer()
    const file = writeTempFile('payload-from-keyword')
    const runner = require('../api/runner/runner')(uploadProps(port))
    runner.setThrowExceptionOnError(false)
    try {
      const output = await runner.run('UploadFileKW', { filePath: file })
      expect(output.payload.transferStatus).toBe('COMPLETED')
      expect(output.payload.size).toBe(Buffer.byteLength('payload-from-keyword'))
      expect(connections).toHaveLength(1)
      expect(connections[0].binary.toString()).toBe('payload-from-keyword')
    } finally {
      fs.unlinkSync(file)
      await runner.close()
      wss.close()
    }
  })

  test('a keyword streams a file as it is being written', async () => {
    const { wss, port, connections } = await startMockUploadServer()
    const file = writeTempFile('')
    const runner = require('../api/runner/runner')(uploadProps(port))
    runner.setThrowExceptionOnError(false)
    const chunks = ['alpha\n', 'beta\n', 'gamma\n']
    try {
      const output = await runner.run('StreamGrowingFileKW', { filePath: file, chunks })
      expect(output.payload.transferStatus).toBe('COMPLETED')
      expect(connections[0].binary.toString()).toBe(chunks.join(''))
    } finally {
      fs.unlinkSync(file)
      await runner.close()
      wss.close()
    }
  })
})
