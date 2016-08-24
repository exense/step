package step.script.selenium;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.InputMessage;
import step.script.AbstractScript;

public class AbstractSeleniumScript extends AbstractScript {
	
	private static final String SELENIUM_DRIVER_ATTRIBUTE = "#SELENIUM_DRIVER#";
	
	protected WebDriver driver;
	
	@Override
	public void beforeCall(AgentTokenWrapper token, InputMessage message) {
		TokenSession session = token.getSession();
		Object driver_ = session.get(SELENIUM_DRIVER_ATTRIBUTE);
		if(driver_!=null) {
			driver = (WebDriver) driver_;
		}
		super.beforeCall(token, message);
	}
	
	protected void setDriver(WebDriver driver) {
		Object driver_ = token.getSession().get(SELENIUM_DRIVER_ATTRIBUTE);
		if(driver_!=null) {
			((WebDriver)driver_).quit();
		}
		this.driver = driver;
		token.getSession().put(SELENIUM_DRIVER_ATTRIBUTE, driver);
	}

	@Override
	public boolean onError(AgentTokenWrapper token, InputMessage message, Exception e) {
		setError("Error in selenium script: "+e.getMessage(), e);

		if(driver!=null && driver instanceof TakesScreenshot) {
			try {
				String base64 = ((TakesScreenshot)driver).getScreenshotAs(OutputType.BASE64);
				Attachment a = new Attachment();
				a.setHexContent(base64);
				a.setName("screenshot.jpg");
				addAttachment(a);
			
				addAttachment(AttachmentHelper.generateAttachmentForException(e));
			} catch(Exception exception) {
				addAttachment(AttachmentHelper.generateAttachmentForException(exception));
			}
		}
		super.onError(token, message, e);
		return true;
	}

	@Override
	public void afterCall(AgentTokenWrapper token, InputMessage message) {
		super.afterCall(token, message);
	}
	
}
