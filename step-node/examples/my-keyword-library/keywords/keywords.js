exports.Open_Chrome = async (input, output, session) => {
  const webdriver = require('selenium-webdriver')

  const driver = await new webdriver.Builder()
    .forBrowser('chrome')
    .build()

  session.driver_wraper = { 'driver': driver, 'close': function() {
	  console.log('[Driver wrapper] Closing selenium driver')
	  this.driver.quit()
  } }
  
    output.send({ result: 'OK' })
}

exports.Sleep  = async (input, output, session, properties) => {
  setTimeout(function(){output.send()}, input['ms'])
}

exports.Google_Search = async (input, output, session, properties) => {
  var googleUrl = properties['google.url'];

  const webdriver = require('selenium-webdriver')
  const { By } = webdriver
  const driver = session.driver_wraper.driver

  session.driver_wraper.driver = driver
  await driver.get(googleUrl)
  await driver.findElement(By.name('q')).sendKeys(input.search + webdriver.Key.ENTER)

  const data = await driver.takeScreenshot()
  output.attach({ name: 'screenshot.png', hexContent: data })
  output.send({ result: 'OK' })
}

exports.Close_Chrome = async (input, output, session) => {
  const driver = session.driver_wraper.driver
  driver.quit()

  output.send({ result: 'OK' })
}

exports.onError = async (exception, input, output, session, properties) => {
	
	if (session.driver_wraper) {
		const data = await session.driver_wraper.driver.takeScreenshot()
		output.attach({ name: 'screenshot_onError.png', hexContent: data })
	}
	
	return true
}
