package step.script.selenium;

import java.io.Closeable;
import java.io.IOException;

import org.openqa.selenium.WebDriver;

public class DriverWrapper implements Closeable {
	
	private WebDriver driver;

	public DriverWrapper(WebDriver driver) {
		super();
		this.driver = driver;
	}

	public WebDriver getWebDriver() {
		return driver;
	}

	@Override
	public void close() throws IOException {
		driver.quit();
	}

}
