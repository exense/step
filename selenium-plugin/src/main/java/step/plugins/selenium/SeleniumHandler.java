package step.plugins.selenium;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.conf.AgentConf;
import step.grid.agent.handler.AgentConfigurationAware;
import step.grid.agent.handler.ClassLoaderMessageHandlerWrapper;
import step.grid.agent.handler.IsolatingURLClassLoader;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.TokenHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.handlers.javahandler.JavaHandler;
import step.handlers.scripthandler.ScriptHandler;

public class SeleniumHandler implements MessageHandler, AgentConfigurationAware {
	
	private static final Logger logger = LoggerFactory.getLogger(SeleniumHandler.class);
	
	Map<String, SeleniumScriptExecutionContext> contexts = new ConcurrentHashMap<>();
	
	public SeleniumHandler() {
		super();
	}
	
	class SeleniumScriptExecutionContext {
		
		ClassLoader seleniumClassLoader;
		
		JavaHandler javaHandler;
		
		ScriptHandler scriptHandler;
		
		MessageHandler targetMessageHandler ;

		public SeleniumScriptExecutionContext(ClassLoader classLoader) {
			super();
			this.seleniumClassLoader = classLoader;
			this.javaHandler = new JavaHandler();
			this.scriptHandler = new ScriptHandler();
		}
	}
	
	@Override
	public void init(AgentConf configuration) {
		loadSeleniumClassLoader(configuration, "2.x");
		loadSeleniumClassLoader(configuration, "3.x");
	}

	protected void loadSeleniumClassLoader(AgentConf configuration, String version) {
		String seleniumLibsPropKey = "plugins.selenium."+version+".libs";
		String seleniumLibs = configuration.getProperties().get(seleniumLibsPropKey);
		
		logger.info("Loading selenium libs from: "+seleniumLibs);
				
		if(seleniumLibs==null) {
			throw new RuntimeException("Agent property '"+seleniumLibsPropKey+"' is not defined. Please define it in your AgentConf.js");
		} else {
			File seleniumLibsFile = new File(seleniumLibs);
			if(seleniumLibsFile.exists()&&seleniumLibsFile.list().length>0) {
				try {
					List<URL> urls = new ArrayList<>();
					TokenHandlerPool.addJarsToUrls(urls, seleniumLibsFile);
					
					logger.debug("Creating isolating classloader with following URLs: "+Arrays.toString(urls.toArray()));
					IsolatingURLClassLoader seleniumClassLoader = new IsolatingURLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader());
					contexts.put(version, new SeleniumScriptExecutionContext(seleniumClassLoader));
				} catch (MalformedURLException e) {
					throw new RuntimeException("Error while loading selenium classloader '"+version+"'",e);
				}
				
			} else {
				throw new RuntimeException("The lib folder of selenium specified by the agent property '"+seleniumLibsPropKey+"' and resolving to '"+seleniumLibsFile.getAbsolutePath()+"' doesn't exist or is empty");
			}
		}
	}
	
	@Override
	public OutputMessage handle(final AgentTokenWrapper token, final InputMessage message) throws Exception {
		String version = message.getProperties().get("selenium.version");
		SeleniumScriptExecutionContext context = contexts.get(version);
		ClassLoader seleniumClassloader = context.seleniumClassLoader;
		
		String scriptLanguage = message.getProperties().get(ScriptHandler.SCRIPT_LANGUAGE);
		MessageHandler targetHandler = scriptLanguage.equals("java")?context.javaHandler:context.scriptHandler;
		
		if(logger.isDebugEnabled()) {
			logger.debug("Handling selenium message using selenium version '"+version+"', classloader: '"+seleniumClassloader.toString()+"' script language: "+scriptLanguage);
		}
		
		ClassLoaderMessageHandlerWrapper classLoaderWrapper = new ClassLoaderMessageHandlerWrapper(seleniumClassloader);
		return classLoaderWrapper.runInContext(new Callable<OutputMessage>() {
			@Override
			public OutputMessage call() throws Exception {		
				classLoaderWrapper.setDelegate(targetHandler);
				return classLoaderWrapper.handle(token, message);
			}
		});
	}

}
