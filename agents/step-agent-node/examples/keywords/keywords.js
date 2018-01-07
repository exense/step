exports.Open_Chrome = async function(input, output, session) {

	var webdriver = require('selenium-webdriver'),
    By = webdriver.By,
    until = webdriver.until;

	var driver = await new webdriver.Builder()
		.forBrowser('chrome')
		.build();
		
	session.driver = driver;

	output.send();
}

exports.Google_Search = async function(input, output, session) {
	try {
		//promise.USE_PROMISE_MANAGER = false;
		var webdriver = require('selenium-webdriver'),
		By = webdriver.By,
		until = webdriver.until;

		var driver = session.driver;

		session.driver = driver;
		await driver.get('http://www.google.com/ncr');
		await driver.findElement(By.name('q')).sendKeys(input.search+webdriver.Key.ENTER);

		var data = await driver.takeScreenshot();
		output.attach({"name":"screenshot.png","hexContent":data});

		//await driver.findElement(By.name('btnGe')).click();
		//await driver.wait(until.titleIs(input.search+' - Google Search'), 1000).then(function() {
			
		//});
		output.send({"result":"OK"});
		//driver.quit();
	} catch (e) {
		output.fail(e);
	}
}