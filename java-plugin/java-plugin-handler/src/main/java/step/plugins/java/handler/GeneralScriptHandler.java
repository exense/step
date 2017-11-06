package step.plugins.java.handler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.bootstrap.RemoteClassPathBuilder;
import step.grid.bootstrap.RemoteClassPathBuilder.RemoteClassPath;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.grid.isolation.ApplicationContextBuilder;
import step.grid.isolation.ClassPathHelper;
import step.plugins.js223.handler.ScriptHandler;

public class GeneralScriptHandler extends AbstractMessageHandler {
			
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
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		String scriptLanguage = message.getProperties().get(ScriptHandler.SCRIPT_LANGUAGE);
		Class<?> handlerClass = scriptLanguage.equals("java")?JavaJarHandler.class:ScriptHandler.class;

		appContextBuilder.pushContextIfAbsent("script-dev", ()->{
			File jar;
			InputStream is = getClass().getClassLoader().getResourceAsStream("script-dev-java.jar");
			try {
				jar = File.createTempFile("script-dev-java-" + UUID.randomUUID(), ".jar");
				Files.copy(is, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new RuntimeException("Error while extracting plugin file", e);
			}
			RemoteClassPath cp = new RemoteClassPath();
			cp.setUrls(ClassPathHelper.forSingleFile(jar));
			return cp;	
		});
		
		RemoteClassPath remoteCp = remoteClassPathBuilder.buildRemoteClassPath(getFileVersionId(ScriptHandler.LIBRARIES_FILE, message.getProperties()));
		appContextBuilder.pushContextIfAbsent(remoteCp.getKey(), remoteCp);
		return messageHandlerPool.get(handlerClass.getName()).handle(token, message);			
	}

}
