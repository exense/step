package step.adapters.selenium;

import org.openqa.selenium.WebDriver;

import step.grid.TokenWrapper;
import step.grid.io.InputMessage;

public interface WebDriverFactory {

	public WebDriver getOrCreateWebDriver(TokenWrapper session, InputMessage message) throws Exception;
}
