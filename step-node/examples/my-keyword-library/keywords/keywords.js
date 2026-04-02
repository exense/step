exports.openChrome = async (input, output, session) => {
  const webdriver = require('selenium-webdriver');

  const driver = await new webdriver.Builder()
    .forBrowser('chrome')
    .build();

  // Add a wrapper of the driver to the session in order to make the driver available to other keywords
  // and implement a close method so the driver is closed when the session is released
  session.set('driver_wrapper', {
    'driver': driver, 'close': async function () {
      console.log('[Driver wrapper] Closing selenium driver');
      await driver.quit();
    }
  })

  output.add('result', 'OK');
}

exports.buyMacbookInOpencart = async (input, output, session, properties) => {
  const webdriver = require('selenium-webdriver')
  const {By} = webdriver
  const driver = session.get('driver_wrapper').driver

  await driver.get(input.url)
  await driver.findElement(By.linkText('Desktops')).click()
  await driver.findElement(By.linkText('Mac (1)')).click()
  await driver.findElement(By.xpath('//*[text()="iMac"]')).click()
  const priceElement = await driver.findElement(By.xpath('//h2[contains(text(),"$")]'))
  const price = await priceElement.getText()
  output.add('price', price)
}

exports.onError = async (exception, input, output, session, properties) => {

  if (session.get('driver_wrapper')) {
    const data = await session.get('driver_wrapper').driver.takeScreenshot()
    output.attach({name: 'screenshot_onError.png', hexContent: data})
  }

  return true
}
