describe('Keyword library test', () => {

  let runner

  beforeAll(() => {
    // Create a keyword runner instance to unitary test the keywords
    runner = require('step-node-agent').runner({})
    runner.setThrowExceptionOnError(true)
  })

  afterAll(async () => {
    // Close the runner to release the session and all associated objects
    await runner.close()
  })

  test('should run the Keyword buyMacBookInOpenCart and verify output', async () => {
    // Call the Keyword Open_Chrome
    let output = await runner.run('openChrome', {})
    expect(output.payload.result).toBe('OK')

    // Call the Keyword buyMacbookInOpencart
    output = await runner.run('buyMacbookInOpencart', {url: 'https://opencart-prf.stepcloud.ch/'})
    expect(output.payload.price).toBe('$122.00')
  }, 10000)
})
