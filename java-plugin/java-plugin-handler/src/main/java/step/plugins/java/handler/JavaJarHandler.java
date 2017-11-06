package step.plugins.java.handler;

import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.bootstrap.RemoteClassPathBuilder;
import step.grid.bootstrap.RemoteClassPathBuilder.RemoteClassPath;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.grid.isolation.ApplicationContextBuilder;
import step.handlers.javahandler.JavaHandler;
import step.plugins.js223.handler.ScriptHandler;

public class JavaJarHandler extends AbstractMessageHandler {
	
	private ApplicationContextBuilder appContextBuilder;
	
	private RemoteClassPathBuilder remoteClassPathBuilder;
	
	private MessageHandlerPool messageHandlerPool;
				
	@Override
	public void init(AgentTokenServices agentTokenServices) {
		super.init(agentTokenServices);
		appContextBuilder = agentTokenServices.getApplicationContextBuilder();	
		messageHandlerPool = new MessageHandlerPool(agentTokenServices);
		remoteClassPathBuilder = new RemoteClassPathBuilder(agentTokenServices.getFileManagerClient());
	}
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, final InputMessage message) throws Exception {
		RemoteClassPath remoteCp = remoteClassPathBuilder.buildRemoteClassPath(getFileVersionId(ScriptHandler.SCRIPT_FILE, message.getProperties()));
		
		appContextBuilder.pushContextIfAbsent(remoteCp.getKey(), remoteCp);
		return messageHandlerPool.get(JavaHandler.class.getName()).handle(token, message);	
	}
}
