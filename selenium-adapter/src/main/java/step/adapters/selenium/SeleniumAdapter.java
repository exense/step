package step.adapters.selenium;

import org.openqa.selenium.WebDriver;

import step.grid.Token;
import step.grid.agent.handler.TokenHandler;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class SeleniumAdapter implements TokenHandler {

	private static final String SELENIUM_DRIVER_ATTRIBUTE = "#SELENIUM_DRIVER#";
	
	public OutputMessage handle(Token token, TokenSession session, InputMessage message) {
		Object driver_ = session.get(SELENIUM_DRIVER_ATTRIBUTE);
		
		if(driver_!=null & driver_ instanceof WebDriver) {
			WebDriver driver = (WebDriver) driver_;
			
		}
		return null;
	}

}
