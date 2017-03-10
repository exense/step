package io.denkbar.step.examples.selenium;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

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

	@Function
	public void Google_Search() throws Exception {
		if(input.containsKey("search")) {
			System.setProperty("webdriver.chrome.driver", "C:/Users/jcomte/Programs/chromedriver_win32_2.27/chromedriver.exe");
			WebDriver driver = new ChromeDriver();
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
			output.setError("Input parameter 'search' not defined");
		}
	}
	
	@Test
	public void test() {
	    ScriptContext ctx = ScriptRunner.getExecutionContext();
	    OutputMessage result = ctx.run("Google_Search","{ \"search\" : \"denkbar step\" }");
	    result.getPayload();
	}

}