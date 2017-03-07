package step.handlers;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.handlers.javahandler.JavaHandler;
import step.handlers.scripthandler.ScriptHandler;

public class GeneralScriptHandler implements MessageHandler {
	
	public static final String SCRIPT_LANGUAGE = "scriptlanguage";
	
	private JavaHandler javaHandler = new JavaHandler();
	
	private ScriptHandler jsr223Handler = new ScriptHandler();
	
	public static final String SCRIPT_FILE = "file";
	public static final String REMOTE_FILE_ID = "remotefile.id";
	public static final String REMOTE_FILE_VERSION = "remotefile.version";
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		String scriptLanguage = message.getProperties().get(ScriptHandler.SCRIPT_LANGUAGE);
		MessageHandler targetHandler = scriptLanguage.equals("java")?javaHandler:jsr223Handler;
		return targetHandler.handle(token, message);
	}

}
