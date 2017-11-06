package step.grid.bootstrap;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerClient.FileVersion;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.grid.isolation.ClassPathHelper;
import step.grid.isolation.ContextManager;

public class BootstrapManager {
	
	ContextManager contextManager;
	
	FileManagerClient fileManager;
	
	AgentTokenServices agentTokenServices;

	public BootstrapManager(AgentTokenServices agentTokenServices, boolean isTechnicalBootstrap) {
		super();
		this.agentTokenServices = agentTokenServices;
		this.fileManager = agentTokenServices.getFileManagerClient();
		this.contextManager = new ContextManager();
	}

	public OutputMessage runBootstraped(AgentTokenWrapper token, InputMessage message, String handlerClass, FileVersionId handlerPackage) throws IOException, Exception {
		String contextKey = setupContext(handlerPackage);
		
		return contextManager.runInContext(contextKey, new Callable<OutputMessage>() {
			@Override
			public OutputMessage call() throws Exception {
				MessageHandlerPool handlerPool = (MessageHandlerPool) contextManager.getCurrentContextObject();
				MessageHandler handler = handlerPool.get(handlerClass);
				return handler.handle(token, message);
			}
		});
	}
	
	public OutputMessage runBootstraped(AgentTokenWrapper token, InputMessage message) throws IOException, Exception {
		return runBootstraped(token, message, message.getHandler(), message.getHandlerPackage());

	}

	protected String setupContext(FileVersionId handlerPackage) throws IOException, Exception {
		String contextKey;
		List<URL> urls;
		Callable<Object> handlerPoolFactory = () -> {return new MessageHandlerPool(agentTokenServices);};
		boolean forceReload;
		if(handlerPackage!=null) {
			FileVersion libFolder = fileManager.requestFileVersion(handlerPackage.getFileId(), handlerPackage.getVersion());
			contextKey = libFolder.getFileId();
			
			forceReload = libFolder.isModified();
			if (libFolder.getFile().isDirectory()) {
				urls = ClassPathHelper.forAllJarsInFolder(libFolder.getFile());
			} else {
				urls = ClassPathHelper.forSingleFile(libFolder.getFile());
			}	
		} else {
			contextKey = "default";
			urls = new ArrayList<>();
			forceReload = false;
		}
		
		contextManager.loadContextIfAbsent(contextKey, urls, handlerPoolFactory, forceReload);
		return contextKey;
	}

}
