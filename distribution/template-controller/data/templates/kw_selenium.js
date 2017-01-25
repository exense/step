// Use the following if you're starting a new driver:
var FirefoxDriver = Java.type("org.openqa.selenium.firefox.FirefoxDriver");
var DriverWrapper = Java.type("step.script.selenium.DriverWrapper")

java.lang.System.getProperties().put("webdriver.gecko.driver", "C:/Users/jcomte/Programs/geckodriver-v0.10.0-win64/geckodriver.exe")

var driver = new FirefoxDriver()
session.put("driver",new DriverWrapper(driver))

// Use the following if you're reusing a driver

// driver = session.get("driver").getWebDriver()

// Example:
// driver.navigate(driver.get(input.getString("url")))