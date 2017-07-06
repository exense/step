package io.denkbar.step.examples.selenium;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import step.grid.io.OutputMessage;
import step.handlers.javahandler.AbstractScript;
import step.handlers.javahandler.Function;
import step.handlers.javahandler.ScriptRunner;
import step.handlers.javahandler.ScriptRunner.ScriptContext;

public class SeleniumKeywordExample extends AbstractScript {
	
	public class DriverWrapper implements Closeable {

		final WebDriver driver;
		
		public DriverWrapper(WebDriver driver) {
			super();
			this.driver = driver;
		}

		@Override
		public void close() throws IOException {
			driver.quit();
		}	
	}
	
	@Function(name="Open Chrome")
	public void openChrome() throws Exception {
		File chromedriverExe = new File(properties.get("chromedriver"));
		if(chromedriverExe.exists()) {
			System.setProperty("webdriver.chrome.driver", chromedriverExe.getAbsolutePath());
			final WebDriver driver = new ChromeDriver();
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			session.put(new DriverWrapper(driver));
		} else {
			output.setError("Unable to find chromedriver.exe in '"+chromedriverExe.getParent() +"'.");
		}
	}
	
	@Function(name="Search in google")
	public void Google_Search() throws Exception {
		if(input.containsKey("search")) {
			final WebDriver driver = session.get(DriverWrapper.class).driver;
			
			driver.get("http://www.google.com");
			
			WebElement searchInput = driver.findElement(By.id("lst-ib"));
			
			String searchString = input.getString("search");
			searchInput.sendKeys(searchString);			
			
			WebElement searchButton = driver.findElement(By.xpath("//button[@name='btnG']"));
			searchButton.click();
			
			WebElement resultCountDiv = driver.findElement(By.xpath("//div/nobr"));
			
			List<WebElement> resultHeaders = driver.findElements(By.xpath("//h3[@class='r']"));
			for(WebElement result:resultHeaders) {
				output.add(result.getText(), result.findElement(By.xpath("..//cite")).getText());
			}
		} else {
			output.setError("Input parameter 'search' not defined");
		}
	}
	
	ScriptContext ctx;
	
	@Before
	public void setUp() {
		Map<String, String> properties = new HashMap<>();
		properties.put("chromedriver", "../ext/bin/chromedriver/chromedriver.exe");
		ctx = ScriptRunner.getExecutionContext(properties);
	}
	
	@Test
	public void test() {
	    OutputMessage result;
	    result = ctx.run("Open Chrome","{ \"search\" : \"denkbar step\" }");
	    result = ctx.run("Search in google","{ \"search\" : \"denkbar step\" }");
	    result = ctx.run("Search in google","{ \"search\" : \"denkbar djigger\" }");
	    Assert.assertNull(result.getError());
	    result.getPayload();
	}
	
	@After
	public void tearDown() {
		ctx.close();
	}

}