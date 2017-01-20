var HtmlUnitDriver = Java.type("org.openqa.selenium.htmlunit.HtmlUnitDriver");
var DriverWrapper = Java.type("step.script.selenium.DriverWrapper")

var driver = new HtmlUnitDriver();
driver.setJavascriptEnabled(true);

session.put("driver",new DriverWrapper(driver))