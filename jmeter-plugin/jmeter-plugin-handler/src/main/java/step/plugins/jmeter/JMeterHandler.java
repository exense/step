package step.plugins.jmeter;

import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.grid.isolation.ApplicationContextBuilder;
import step.grid.isolation.LocalResourceApplicationContextFactory;
import step.grid.isolation.RemoteApplicationContextFactory;

public class JMeterHandler extends AbstractMessageHandler {
	
	private ApplicationContextBuilder appContextBuilder;
		
	private MessageHandlerPool messageHandlerPool;
		
	@Override
	public void init(AgentTokenServices agentTokenServices) {
		super.init(agentTokenServices);
		appContextBuilder = agentTokenServices.getApplicationContextBuilder();	
		messageHandlerPool = new MessageHandlerPool(agentTokenServices);
	}
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {

		RemoteApplicationContextFactory jmeterLibrariesContext = new RemoteApplicationContextFactory(token.getServices().getFileManagerClient(), getFileVersionId("$jmeter.libraries", message.getProperties()));
		appContextBuilder.pushContext(jmeterLibrariesContext);

		LocalResourceApplicationContextFactory jmeterLocalHandlerContext = new LocalResourceApplicationContextFactory(getClass().getClassLoader(), "jmeter-plugin-local-handler.jar");
		appContextBuilder.pushContext(jmeterLocalHandlerContext);
		
		return messageHandlerPool.get("step.plugins.jmeter.JMeterLocalHandler", appContextBuilder.getCurrentContext().getClassLoader()).handle(token, message);			
	}
}
