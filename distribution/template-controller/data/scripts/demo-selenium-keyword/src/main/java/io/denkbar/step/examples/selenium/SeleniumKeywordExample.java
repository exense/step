package io.denkbar.step.examples.selenium;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
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
	
	@Function
	public void Google_Search() throws Exception {
		if(input.containsKey("search")) {
			File chromedriverExe = new File("../ext/bin/chromedriver/chromedriver.exe");
			if(chromedriverExe.exists()) {
				System.setProperty("webdriver.chrome.driver", chromedriverExe.getAbsolutePath());
				final WebDriver driver = new ChromeDriver();
				
				session.put("driver", new DriverWrapper(driver));
				
				driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
				
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
				output.setError("Unable to find chromedriver.exe in '"+chromedriverExe.getParent() +"'.");
			}
			
		} else {
			output.setError("Input parameter 'search' not defined");
		}
	}
	
	ScriptContext ctx = ScriptRunner.getExecutionContext();
	
	@Test
	public void test() {
	    OutputMessage result = ctx.run(SeleniumKeywordExample.class,"Google_Search","{ \"search\" : \"denkbar step\" }");
	    Assert.assertNull(result.getError());
	    result.getPayload();
	}
	
	@After
	public void tearDown() {
		ctx.close();
	}

}