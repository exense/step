const runner = require('step-node-agent').runner({'google.url':'http://www.google.com/ncr'})
const assert = require('assert')

;(async () => {
  var output = await runner.run('Open_Chrome', {})
  assert.equal(output.payload.result, 'OK')
  output = await runner.run('Google_Search', { search: 'djigger' })
  assert.equal(output.payload.result, 'OK')
  await runner.run('Google_Search', { search: 'exense step' })
  assert.equal(output.payload.result, 'OK')
})()
