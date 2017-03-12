package step.plugins.jmeter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import step.common.isolation.ClassPathHelper;
import step.common.isolation.ContextManager;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.conf.AgentConf;
import step.grid.agent.handler.AgentContextAware;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.TokenHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class JMeterHandler implements MessageHandler, AgentContextAware {

	AgentConf conf;
		
	ContextManager contextManager;
	
	public JMeterHandler() {
		super();
	}
	
	@Override
	public void init(AgentTokenServices tokenServices) {
		contextManager = tokenServices.getContextManager();
		
		String jmeterHome = tokenServices.getAgentProperties().get("plugins.jmeter.home");
		if(jmeterHome==null) {
			throw new RuntimeException("Agent property 'plugins.jmeter.home' is not defined. Please define it in your AgentConf.js");
		} else {
			File jmeterHomeFile = new File(jmeterHome);
			if(jmeterHomeFile.exists()&&jmeterHomeFile.list().length>0) {
				List<URL> urls = new ArrayList<>();
				urls.addAll(ClassPathHelper.forAllJarsInFolder(new File(jmeterHome+"/lib")));
				urls.addAll(ClassPathHelper.forAllJarsInFolder(new File(jmeterHome+"/bin")));
				
				for(String cpEntry:System.getProperties().getProperty("java.class.path").split(";")) {
					if(cpEntry.contains("jmeter-plugin")) {
						urls.addAll(ClassPathHelper.forSingleFile(new File(cpEntry)));
					}
				}
				try {
					contextManager.loadContext("jmeter", urls, true,new Callable<Object>() {
						@Override
						public Object call() throws Exception {
							// Use the token handler pool to force the class to be loaded by the context class loader and not the app-classloader 
							TokenHandlerPool localTokenHandlerPool = new TokenHandlerPool(tokenServices);
							return localTokenHandlerPool;
						}
					});
				} catch (Exception e) {
					throw new RuntimeException("Error while loading jmeter context using classpath :"+urls.toString(),e);
				}
			} else {
				throw new RuntimeException("The home folder of JMeter specified by the agent property 'plugins.jmeter.home' and resolving to '"+jmeterHomeFile.getAbsolutePath()+"' doesn't exist or is empty");
			}
		}
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		return contextManager.runInContext("jmeter", new Callable<OutputMessage>() {
			@Override
			public OutputMessage call() throws Exception {
				TokenHandlerPool handlerPool = (TokenHandlerPool) ContextManager.getCurrentContextObject();
				MessageHandler localHandler = handlerPool.get("class:step.plugins.jmeter.JMeterLocalHandler");
				return localHandler.handle(token, message);
			}
		});
	}


}
