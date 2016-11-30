java.lang.System.getProperties().put("webdriver.chrome.driver", "C:/Users/jcomte/Programs/chromedriver_win32_2.25/chromedriver.exe")

var DriverWrapper = Java.type("step.script.selenium.DriverWrapper")
var ChromeDriver = Java.type("org.openqa.selenium.chrome.ChromeDriver")
driverWrapper = new DriverWrapper(new ChromeDriver())
session.put("driver",driverWrapper)