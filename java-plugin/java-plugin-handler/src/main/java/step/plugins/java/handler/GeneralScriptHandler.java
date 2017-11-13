package step.plugins.java.handler;

import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.LocalResourceApplicationContextFactory;
import step.grid.contextbuilder.RemoteApplicationContextFactory;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.plugins.js223.handler.ScriptHandler;

public class GeneralScriptHandler extends AbstractMessageHandler {
			
	private ApplicationContextBuilder appContextBuilder;
	
	private MessageHandlerPool messageHandlerPool;
	
	private FileManagerClient fileManagerClient;
		
	@Override
	public void init(AgentTokenServices agentTokenServices) {
		super.init(agentTokenServices);
		appContextBuilder = agentTokenServices.getApplicationContextBuilder();	
		messageHandlerPool = new MessageHandlerPool(agentTokenServices);
		fileManagerClient = agentTokenServices.getFileManagerClient();
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		LocalResourceApplicationContextFactory scriptApiContext = new LocalResourceApplicationContextFactory(getClass().getClassLoader(), "script-dev-java.jar");
		appContextBuilder.pushContext(scriptApiContext);
		
		FileVersionId librariesFileVersion = getFileVersionId(ScriptHandler.LIBRARIES_FILE, message.getProperties());
		if(librariesFileVersion!=null) {
			RemoteApplicationContextFactory librariesContext = new RemoteApplicationContextFactory(fileManagerClient, librariesFileVersion);
			appContextBuilder.pushContext(librariesContext);			
		}
		
		String scriptLanguage = message.getProperties().get(ScriptHandler.SCRIPT_LANGUAGE);
		Class<?> handlerClass = scriptLanguage.equals("java")?JavaJarHandler.class:ScriptHandler.class;
		return messageHandlerPool.get(handlerClass.getName()).handle(token, message);			
	}

}
