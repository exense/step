var FirefoxDriver = Java.type("org.openqa.selenium.firefox.FirefoxDriver");
var DriverWrapper = Java.type("step.script.selenium.DriverWrapper")

java.lang.System.getProperties().put("webdriver.gecko.driver", properties.get("selenium.webdriver.gecko.driver"))
java.lang.System.getProperties().put("webdriver.firefox.bin", properties.get("selenium.webdriver.firefox.bin"))


var driver = new FirefoxDriver()
session.put("driver",new DriverWrapper(driver))