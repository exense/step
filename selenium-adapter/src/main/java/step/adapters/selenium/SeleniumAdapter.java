package step.adapters.selenium;

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

import step.grid.Token;
import step.grid.agent.handler.TokenHandler;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class SeleniumAdapter implements TokenHandler {

	private static final String SELENIUM_DRIVER_ATTRIBUTE = "#SELENIUM_DRIVER#";
	
	private ExecutorService executor = Executors.newCachedThreadPool();
	
	public OutputMessage handle(Token token, TokenSession session, final InputMessage message) throws Exception {
		Object driver_ = session.get(SELENIUM_DRIVER_ATTRIBUTE);
		
		final WebDriver driver;
		if(driver_!=null & driver_ instanceof WebDriver) {
			driver = (WebDriver) driver_;
		} else {
			// TODO parameter: selenium.webdriver.class
			String webDriverClassname = "";
			driver = (WebDriver) Class.forName(webDriverClassname).newInstance();
		}
			
		
		// TODO parameter: selenium.calltimeout
		long timeout = 60000;
		// TODO parameter: selenium.scriptdir
		String seleniumScriptsDir = "scripts/";
		
		String scriptPath = seleniumScriptsDir;// + input.getKeyword() + "/";
		
		File f = new File(scriptPath);
		final URLClassLoader cl = new URLClassLoader(new URL[] { f.toURI().toURL() }, this.getClass().getClassLoader());
		try {
			final Class<?> scriptClass = cl.loadClass(message.getFunction());
						
			if(SeleniumScript.class.isAssignableFrom(scriptClass)) {
				@SuppressWarnings("unchecked")
				final SeleniumScript script = (SeleniumScript) scriptClass.newInstance();
				Future<OutputMessage> future = executor.submit(new Callable<OutputMessage>() {

					public OutputMessage call() throws Exception {
						Thread.currentThread().setContextClassLoader(cl);
						return script.run(driver, message);						
					}
					
				});
				try {
					OutputMessage output = future.get(timeout, TimeUnit.MILLISECONDS);
				} catch(TimeoutException e) {
					future.cancel(true);
//					handleError("A timeout occurred while calling script "+scriptClass.getCanonicalName() +" from script directory "+scriptPath+
//							". The timeout set to selenium.calltimeout="+timeout+"ms.", null, outputBuilder, resource);
				} catch(ExecutionException e) {
//					handleError("A error occurred while calling script "+scriptClass.getCanonicalName() +" from script directory "+scriptPath+
//							". See attachment.", e.getCause(), outputBuilder, resource);
				}
			}
		} finally {
			cl.close();
		}
		
		return null;
	}

}
