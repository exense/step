const http = require('http')
const {
  createLiveReporting,
  RestUploadingLiveMetricDestination,
  DiscardingLiveMetricDestination,
  MetricSamplesCollector,
  CounterMetric,
  GaugeMetric,
  HistogramMetric,
} = require('../api/controllers/live-reporting')

// Mock controller endpoint that captures POSTed metric-sample batches.
function startMockServer(onMetrics) {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      if (req.method === 'POST' && req.url.includes('/metrics')) {
        let body = ''
        req.on('data', (c) => { body += c })
        req.on('end', () => {
          onMetrics(JSON.parse(body), req.url)
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

describe('metrics - instrument flush semantics', () => {
  test('CounterMetric flush captures interval count/sum and running total', () => {
    const c = new CounterMetric('requests', { route: '/x' })
    c.increment()
    c.increment(3)
    const s = c.flush()
    expect(s.type).toBe('COUNTER')
    expect(s.name).toBe('requests')
    expect(s.labels).toEqual({ route: '/x' })
    expect(s.count).toBe(2)     // two increment calls
    expect(s.sum).toBe(4)       // 1 + 3 this interval
    expect(s.min).toBe(0)       // total before this interval
    expect(s.max).toBe(4)       // running total
    expect(s.last).toBe(4)
    expect(s.distribution).toBeNull()
    // After flush, interval resets but the total carries over
    c.increment(2)
    const s2 = c.flush()
    expect(s2.count).toBe(1)
    expect(s2.sum).toBe(2)
    expect(s2.min).toBe(4)      // total before this interval
    expect(s2.max).toBe(6)
  })

  test('CounterMetric rejects negative increments', () => {
    expect(() => new CounterMetric('c').increment(-1)).toThrow('non-negative')
  })

  test('GaugeMetric flush captures distribution and retains last across flushes', () => {
    const g = new GaugeMetric('queue')
    g.observe(12)
    g.observe(25)
    g.observe(23)
    const s = g.flush()
    expect(s.type).toBe('GAUGE')
    expect(s.count).toBe(3)
    expect(s.sum).toBe(60)
    expect(s.min).toBe(12)
    expect(s.max).toBe(25)
    expect(s.last).toBe(23)
    // buckets of width 10: 12->10, 25->20, 23->20
    expect(s.distribution).toEqual({ 10: 1, 20: 2 })
    // After flush: accumulators reset, but `last` is retained; empty flush reports zeros
    const empty = g.flush()
    expect(empty.count).toBe(0)
    expect(empty.min).toBe(0)
    expect(empty.max).toBe(0)
    expect(empty.last).toBe(23)
    expect(empty.distribution).toEqual({})
  })

  test('HistogramMetric reports type HISTOGRAM', () => {
    const h = new HistogramMetric('resp')
    h.observe(5)
    expect(h.flush().type).toBe('HISTOGRAM')
  })
})

describe('metrics - sample collector', () => {
  test('first observation starts the clock; later observations flush past the interval', () => {
    const forwarded = []
    const collector = new MetricSamplesCollector((s) => forwarded.push(s), 1000)
    const c = new CounterMetric('c')
    collector.register(c)
    // Drive observation timestamps explicitly (no real time needed). Use realistic epoch-ms values:
    // a timestamp of 0 collides with the "clock not started" sentinel (same as the Java impl).
    const t0 = 1_000_000
    c.increment(1, t0)         // first obs: starts clock, no flush
    c.increment(1, t0 + 500)   // within interval: no flush
    expect(forwarded).toHaveLength(0)
    c.increment(1, t0 + 1000)  // >= 1000ms since clock start: flush
    expect(forwarded).toHaveLength(1)
    expect(forwarded[0].count).toBe(3) // all three increments captured in the flush
  })

  test('close() performs a final flush of accumulated values', () => {
    const forwarded = []
    const collector = new MetricSamplesCollector((s) => forwarded.push(s), 60000)
    const c = new CounterMetric('c')
    collector.register(c)
    c.increment(1, 0)
    c.increment(1, 10)
    expect(forwarded).toHaveLength(0) // nothing flushed yet (within interval)
    collector.close()
    expect(forwarded).toHaveLength(1)
    expect(forwarded[0].count).toBe(2)
  })

  test('a metric with no observations produces no final sample', () => {
    const forwarded = []
    const collector = new MetricSamplesCollector((s) => forwarded.push(s), 60000)
    collector.register(new CounterMetric('idle'))
    collector.close()
    expect(forwarded).toHaveLength(0)
  })
})

describe('metrics - destination wiring', () => {
  test('no controller context => discarding destination, API is a safe no-op', async () => {
    const lr = createLiveReporting({})
    expect(lr.metrics.destination).toBeInstanceOf(DiscardingLiveMetricDestination)
    const counter = lr.metrics.registerCounter('x')
    counter.increment()
    await expect(lr.close()).resolves.toBeUndefined()
  })

  test('contextId + url => REST destination at the /metrics endpoint', () => {
    const lr = createLiveReporting({
      '$liveReporting.contextId': 'ctx-1',
      'step.reporting.url': 'http://localhost:8080',
    })
    expect(lr.metrics.destination).toBeInstanceOf(RestUploadingLiveMetricDestination)
    expect(lr.metrics.destination.endpointUrl).toBe('http://localhost:8080/rest/live-reporting/ctx-1/metrics')
  })
})

describe('metrics - REST destination POST', () => {
  test('flushes registered metrics to the controller on close', async () => {
    const received = []
    const { server, port } = await startMockServer((metrics, url) => received.push({ metrics, url }))
    try {
      const dest = new RestUploadingLiveMetricDestination(
        `http://localhost:${port}/rest/live-reporting/ctx/metrics`,
        { flushIntervalMs: 60000 }
      )
      const c = new CounterMetric('requests')
      dest.accept(c)
      c.increment(5)
      await dest.close()
    } finally {
      server.close()
    }
    expect(received).toHaveLength(1)
    expect(received[0].url).toBe('/rest/live-reporting/ctx/metrics')
    expect(received[0].metrics).toHaveLength(1)
    expect(received[0].metrics[0].name).toBe('requests')
    expect(received[0].metrics[0].type).toBe('COUNTER')
    expect(received[0].metrics[0].sum).toBe(5)
  })
})

describe('metrics - end to end through the forked agent', () => {
  test('metrics registered in a keyword are POSTed to the controller', async () => {
    const received = []
    const { server, port } = await startMockServer((metrics, url) => received.push({ metrics, url }))
    const runner = require('../api/runner/runner')({
      '$liveReporting.contextId': 'ctx-e2e',
      'step.reporting.url': `http://localhost:${port}`,
    })
    runner.setThrowExceptionOnError(false)
    try {
      const output = await runner.run('LiveMetricKW', {})
      expect(output.payload.ok).toBe(true)
      const all = received.flatMap((r) => r.metrics)
      const byName = Object.fromEntries(all.map((m) => [m.name, m]))
      expect(byName.requests.type).toBe('COUNTER')
      expect(byName.requests.sum).toBe(4) // 1 + 3
      expect(byName.queueDepth.type).toBe('GAUGE')
      expect(byName.respTimeMs.type).toBe('HISTOGRAM')
      expect(byName.respTimeMs.count).toBe(2)
      expect(received[0].url).toBe('/rest/live-reporting/ctx-e2e/metrics')
    } finally {
      await runner.close()
      server.close()
    }
  })
})
