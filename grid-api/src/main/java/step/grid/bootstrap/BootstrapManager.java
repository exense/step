package step.grid.bootstrap;

import java.io.IOException;
import java.util.concurrent.Callable;

import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.RemoteApplicationContextFactory;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class BootstrapManager {
	
	ApplicationContextBuilder contextBuilder;
	
	FileManagerClient fileManager;
	
	AgentTokenServices agentTokenServices;

	public BootstrapManager(AgentTokenServices agentTokenServices, boolean isTechnicalBootstrap) {
		super();
		this.agentTokenServices = agentTokenServices;
		this.fileManager = agentTokenServices.getFileManagerClient();
		this.contextBuilder = new ApplicationContextBuilder();
	}

	public OutputMessage runBootstraped(AgentTokenWrapper token, InputMessage message, String handlerClass, FileVersionId handlerPackage) throws IOException, Exception {
		contextBuilder.resetContext();
		if(message.getHandlerPackage()!=null) {
			contextBuilder.pushContext(new RemoteApplicationContextFactory(fileManager, message.getHandlerPackage()));			
		}
		return contextBuilder.runInContext(new Callable<OutputMessage>() {
			@Override
			public OutputMessage call() throws Exception {
				MessageHandlerPool handlerPool = (MessageHandlerPool) contextBuilder.getCurrentContext().get("handlerPool");
				if(handlerPool == null) {
					handlerPool = new MessageHandlerPool(agentTokenServices);
					contextBuilder.getCurrentContext().put("handlerPool", handlerPool);
				}
				MessageHandler handler = handlerPool.get(handlerClass);
				return handler.handle(token, message);
			}
		});
	}
	
	public OutputMessage runBootstraped(AgentTokenWrapper token, InputMessage message) throws IOException, Exception {
		return runBootstraped(token, message, message.getHandler(), message.getHandlerPackage());

	}
}
