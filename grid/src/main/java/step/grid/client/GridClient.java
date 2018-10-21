package step.grid.client;

import java.io.Closeable;
import java.util.Map;

import javax.json.JsonObject;

import step.grid.GridFileService;
import step.grid.TokenWrapper;
import step.grid.client.GridClientImpl.AgentCommunicationException;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public interface GridClient extends Closeable, GridFileService {

	public TokenWrapper getLocalTokenHandle();
	
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession) throws AgentCommunicationException;
	
	public OutputMessage call(TokenWrapper tokenWrapper, String function, JsonObject argument, String handler, FileVersionId handlerPackage, Map<String,String> properties, int callTimeout) throws Exception;
	
	public void returnTokenHandle(TokenWrapper tokenWrapper) throws AgentCommunicationException;
	
	public void close();
}
