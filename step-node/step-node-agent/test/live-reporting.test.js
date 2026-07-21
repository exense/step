// Generic live-reporting tests: the channel container and the reporting-URL/property resolution that
// is shared by all channels. Channel-specific behaviour lives in live-reporting-{measures,metrics,uploads}.test.js.
const {
  createLiveReporting,
  LiveMeasures,
  LiveMetrics,
  StreamingUploads,
  DiscardingLiveMeasureDestination,
  DiscardingLiveMetricDestination,
} = require('../api/controllers/live-reporting')

describe('live reporting - container', () => {
  test('createLiveReporting exposes the measures, metrics and fileUploads channels', () => {
    const lr = createLiveReporting({})
    expect(lr.measures).toBeInstanceOf(LiveMeasures)
    expect(lr.metrics).toBeInstanceOf(LiveMetrics)
    expect(lr.fileUploads).toBeInstanceOf(StreamingUploads)
  })

  test('with no controller context every channel is a safe discarding no-op', async () => {
    const lr = createLiveReporting({})
    expect(lr.measures.destination).toBeInstanceOf(DiscardingLiveMeasureDestination)
    expect(lr.metrics.destination).toBeInstanceOf(DiscardingLiveMetricDestination)
    // API calls must not throw even though nothing is sent anywhere
    lr.measures.startMeasure('x')
    lr.measures.stopMeasure()
    lr.metrics.registerCounter('c').increment()
    await expect(lr.close()).resolves.toBeUndefined()
  })
})

describe('live reporting - reporting URL resolution (shared by all REST channels)', () => {
  test('agent override step.reporting.url takes precedence, and trailing slash is stripped', () => {
    const lr = createLiveReporting({
      '$liveReporting.contextId': 'ctx',
      '$liveReporting.controllerUrl': 'http://controller:9090',
      'step.reporting.url': 'http://agent-override:8080/',
    })
    expect(lr.measures.destination.endpointUrl).toBe('http://agent-override:8080/rest/live-reporting/ctx/measures')
    expect(lr.metrics.destination.endpointUrl).toBe('http://agent-override:8080/rest/live-reporting/ctx/metrics')
  })

  test('falls back to $liveReporting.controllerUrl when no agent override is present', () => {
    const lr = createLiveReporting({
      '$liveReporting.contextId': 'ctx-2',
      '$liveReporting.controllerUrl': 'http://controller:9090',
    })
    expect(lr.measures.destination.endpointUrl).toBe('http://controller:9090/rest/live-reporting/ctx-2/measures')
    expect(lr.metrics.destination.endpointUrl).toBe('http://controller:9090/rest/live-reporting/ctx-2/metrics')
  })

  test('an invalid agent override URL disables the channels instead of crashing', () => {
    const lr = createLiveReporting({
      '$liveReporting.contextId': 'ctx-3',
      'step.reporting.url': 'not-a-url',
    })
    expect(lr.measures.destination).toBeInstanceOf(DiscardingLiveMeasureDestination)
    expect(lr.metrics.destination).toBeInstanceOf(DiscardingLiveMetricDestination)
  })

  test('a context id but no resolvable URL disables the channels', () => {
    const lr = createLiveReporting({ '$liveReporting.contextId': 'ctx-4' })
    expect(lr.measures.destination).toBeInstanceOf(DiscardingLiveMeasureDestination)
    expect(lr.metrics.destination).toBeInstanceOf(DiscardingLiveMetricDestination)
  })
})
