var FirefoxBinary = Java.type("org.openqa.selenium.firefox.FirefoxBinary");
var FirefoxProfile = Java.type("org.openqa.selenium.firefox.FirefoxProfile");
var FirefoxDriver = Java.type("org.openqa.selenium.firefox.FirefoxDriver");

var File =  Java.type("java.io.File");

java.lang.System.getProperties().put("webdriver.gecko.driver", "C:/Users/jcomte/git/step/distribution/template-agent-win32/bin/geckodriver/geckodriver.exe")
//java.lang.System.getProperties().put("webdriver.firefox.bin", "C:/Users/jcomte/git/step/distribution/template-agent-win32/bin/FirefoxPortable/FirefoxPortable.exe")

var DriverWrapper = Java.type("step.script.selenium.DriverWrapper")
var driver = new FirefoxDriver()
//var driver = new FirefoxDriver(new FirefoxBinary(new File("")),new FirefoxProfile())

driverWrapper = new DriverWrapper(driver)
session.put("driver",driverWrapper)