package step.adapters.selenium;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public abstract class SeleniumAdapter implements MessageHandler {

	private static final String SELENIUM_DRIVER_ATTRIBUTE = "#SELENIUM_DRIVER#";
	
	private ExecutorService executor = Executors.newCachedThreadPool();
	
	private String lookupProperty(String key, AgentTokenWrapper token, InputMessage message) {
		String tokenProperty = token.getProperties()!=null?token.getProperties().get(key):null;
		if(tokenProperty!=null) {
			return tokenProperty;
		} else {
			return message.getProperties()!=null?message.getProperties().get(key):null;
		}
	}
	
	public OutputMessage handle(AgentTokenWrapper token, final InputMessage message) throws Exception {
		final WebDriver driver = getOrCreateWebDriver(token.getSession());
		
		try {
			final Class<?> scriptClass = cl.loadClass(message.getFunction());
						
			if(SeleniumScript.class.isAssignableFrom(scriptClass)) {
				final SeleniumScript script = (SeleniumScript) scriptClass.newInstance();
				
				Future<OutputMessage> future = executor.submit(new Callable<OutputMessage>() {
					public OutputMessage call() throws Exception {
						Thread.currentThread().setContextClassLoader(cl);
						return script.run(driver, message);						
					}
				});
				try {
					OutputMessage output = future.get(timeout, TimeUnit.MILLISECONDS);
					return output;
				} catch(TimeoutException e) {
					future.cancel(true);
					throw new Exception("A timeout occurred while calling script "+scriptClass.getCanonicalName()+". The timeout set to selenium.calltimeout="+timeout+"ms.");
				} catch(ExecutionException e) {
					throw e;
				}
			} else {
				throw new Exception("The class '"+scriptClass.getName()+"' doesn't implement "+SeleniumScript.class.getName());
			}
		} finally {
			if(cl instanceof Closeable) {
				((Closeable)cl).close();
			}
		}
	}

	private WebDriver getOrCreateWebDriver(AgentTokenWrapper token, InputMessage message)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		
		String webDriverFactoryClassname = lookupProperty("selenium.webdriverfactory", token, message);
		
		
		final WebDriver driver;
		Object driver_ = session.get(SELENIUM_DRIVER_ATTRIBUTE);
		if(driver_!=null & driver_ instanceof WebDriver) {
			driver = (WebDriver) driver_;
		} else {
			// TODO factory
			driver = new FirefoxDriver(
					new FirefoxBinary(new File("C:\\Users\\EX3CV\\Programs\\Firefox\\FirefoxPortable.exe")),
					new FirefoxProfile());
			session.put(SELENIUM_DRIVER_ATTRIBUTE, driver);
		}
		return driver;
	}
	
	public abstract OutputMessage run(WebDriver driver, InputMessage input) throws Exception;


}
