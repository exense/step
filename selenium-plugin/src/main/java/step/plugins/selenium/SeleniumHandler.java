package step.plugins.selenium;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.common.isolation.ClassPathHelper;
import step.common.isolation.ContextManager;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AgentContextAware;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.handlers.GeneralScriptHandler;

public class SeleniumHandler implements MessageHandler, AgentContextAware {
	
	protected static final String SELENIUM_VERSION = "$selenium.version";

	private static final Logger logger = LoggerFactory.getLogger(SeleniumHandler.class);
	
	protected ContextManager contextManager;
	
	@Override
	public void init(AgentTokenServices agentTokenServices) {
		Map<String, String> agentProperties = agentTokenServices.getAgentProperties();
		contextManager = agentTokenServices.getContextManager();
		
		for(Map.Entry<String, String> e:agentProperties.entrySet()) {
			if(e.getKey().startsWith("plugins.selenium.libs.")) {
				String version = e.getKey().substring("plugins.selenium.libs.".length());
				String contextKey = getContextKey(version);
				File libFolder = new File(e.getValue());
				try {
					contextManager.loadContext(contextKey, ClassPathHelper.forAllJarsInFolder(libFolder), true, new Callable<Object>() {
						
						@Override
						public Object call() throws Exception {
							return new GeneralScriptHandler();
						}
					});
				} catch (Exception e1) {
					throw new RuntimeException("Error while loading context for selenium version "+version+" using lib folder "+libFolder.getAbsolutePath(), e1);
				}				
			}	
		};
	}

	private String getContextKey(String version) {
		return "selenium_libs_"+version;
	}
	
	@Override
	public OutputMessage handle(final AgentTokenWrapper token, final InputMessage message) throws Exception {
		String version = message.getProperties().get(SELENIUM_VERSION);
		if(logger.isDebugEnabled()) {
			logger.debug("Handling selenium message using selenium version '"+version);
		}

		return contextManager.runInContext(getContextKey(version), new Callable<OutputMessage>() {
			@Override
			public OutputMessage call() throws Exception {
				GeneralScriptHandler targetHandler = (GeneralScriptHandler) ContextManager.getCurrentContextObject();
				return targetHandler.handle(token, message);
			}
		});
	}

}
