const http = require('http')
const {
  createLiveReporting,
  RestUploadingLiveMeasureDestination,
} = require('../api/controllers/live-reporting')

// Spins up a throwaway HTTP server that captures POSTed measure batches.
function startMockServer(onMeasures) {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      if (req.method === 'POST' && req.url.includes('/measures')) {
        let body = ''
        req.on('data', (c) => { body += c })
        req.on('end', () => {
          onMeasures(JSON.parse(body), req.url, req.headers)
          res.statusCode = 204
          res.end()
        })
      } else {
        res.statusCode = 404
        res.end()
      }
    })
    server.listen(0, () => resolve({ server, port: server.address().port }))
  })
}

describe('measures - API', () => {
  test('stopMeasure without a matching startMeasure throws', () => {
    const lr = createLiveReporting({})
    expect(() => lr.measures.stopMeasure()).toThrow('Unbalanced measures stack')
  })

  test('startMeasure/stopMeasure submits a measure with computed duration and begin', async () => {
    const accepted = []
    const lr = createLiveReporting({})
    lr.measures.destination = { accept: (m) => accepted.push(m), close: async () => {} }
    lr.measures.startMeasure('step1')
    await new Promise((r) => setTimeout(r, 10))
    lr.measures.stopMeasure({ status: 'PASSED', data: { a: 1 } })
    expect(accepted).toHaveLength(1)
    expect(accepted[0].name).toBe('step1')
    expect(accepted[0].status).toBe('PASSED')
    expect(accepted[0].data).toEqual({ a: 1 })
    expect(accepted[0].duration).toBeGreaterThanOrEqual(10)
    expect(typeof accepted[0].begin).toBe('number')
  })
})

describe('measures - REST destination', () => {
  test('endpoint targets the /measures path (trailing slash stripped)', () => {
    const lr = createLiveReporting({
      '$liveReporting.contextId': 'ctx-1',
      'step.reporting.url': 'http://localhost:8080/',
    })
    expect(lr.measures.destination).toBeInstanceOf(RestUploadingLiveMeasureDestination)
    expect(lr.measures.destination.endpointUrl).toBe('http://localhost:8080/rest/live-reporting/ctx-1/measures')
  })

  test('flushes buffered measures to the controller as a JSON batch', async () => {
    const received = []
    const { server, port } = await startMockServer((measures, url) => received.push({ measures, url }))
    try {
      const dest = new RestUploadingLiveMeasureDestination(
        `http://localhost:${port}/rest/live-reporting/ctx/measures`,
        { flushIntervalMs: 60000 } // rely on close() to flush, not the timer
      )
      dest.accept({ name: 'm1', begin: 1, duration: 10, status: 'PASSED' })
      dest.accept({ name: 'm2', begin: 2, duration: 20, status: 'FAILED' })
      await dest.close()
    } finally {
      server.close()
    }
    expect(received).toHaveLength(1)
    expect(received[0].url).toBe('/rest/live-reporting/ctx/measures')
    expect(received[0].measures.map((m) => m.name)).toEqual(['m1', 'm2'])
  })

  test('a full batch is sent immediately without waiting for the flush interval', async () => {
    const received = []
    const { server, port } = await startMockServer((measures) => received.push(measures))
    try {
      const dest = new RestUploadingLiveMeasureDestination(
        `http://localhost:${port}/rest/live-reporting/ctx/measures`,
        { batchSize: 2, flushIntervalMs: 60000 }
      )
      dest.accept({ name: 'm1', begin: 1, duration: 10, status: 'PASSED' })
      dest.accept({ name: 'm2', begin: 2, duration: 20, status: 'PASSED' }) // reaches batchSize -> flush
      // wait for the in-flight request to land
      await new Promise((r) => setTimeout(r, 100))
      expect(received).toHaveLength(1)
      expect(received[0]).toHaveLength(2)
      await dest.close()
    } finally {
      server.close()
    }
  })
})

describe('measures - end to end through the forked agent', () => {
  test('measures recorded via liveReporting are POSTed to the controller', async () => {
    const received = []
    const { server, port } = await startMockServer((measures, url) => received.push({ measures, url }))
    const runner = require('../api/runner/runner')({
      '$liveReporting.contextId': 'ctx-e2e',
      'step.reporting.url': `http://localhost:${port}`,
    })
    runner.setThrowExceptionOnError(false)
    try {
      const output = await runner.run('LiveMeasureKW', {})
      expect(output.payload.hasLiveReporting).toBe(true)
      const allMeasures = received.flatMap((r) => r.measures)
      expect(allMeasures.map((m) => m.name)).toEqual(
        expect.arrayContaining(['live-step', 'live-pre-timed'])
      )
      expect(received[0].url).toBe('/rest/live-reporting/ctx-e2e/measures')
    } finally {
      await runner.close()
      server.close()
    }
  })
})
