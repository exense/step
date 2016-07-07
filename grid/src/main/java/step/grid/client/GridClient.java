package step.grid.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.Grid;
import step.grid.io.AdapterMessageMarshaller;
import step.grid.io.Input;
import step.grid.io.Output;

public class GridClient {
	
	private static final Logger logger = LoggerFactory.getLogger(GridClient.class);
	
	public static final String SELECTION_CRITERION_THREAD = "#THREADID#";
	
	private Grid adapterGrid;
	
	private Client client;
	
	private AdapterMessageMarshaller<Input> inputMarshaller;
	
	private AdapterMessageMarshaller<Output> outputMarshaller;
	
	public GridClient() {
		super();
	}

	public GridClient(Grid adapterGrid) {
		super();
		
		this.adapterGrid = adapterGrid;
		
		client = ClientBuilder.newClient();
		inputMarshaller = new AdapterMessageMarshaller<Input>(Input.class);
		outputMarshaller = new AdapterMessageMarshaller<Output>(Output.class);
	}

//	public ProcessInputResponse processInput(GridSession adapterSession, Keyword configuration, Input input, UUID permitId, Object owner) throws Exception {		
//		
//		ProcessInputResponse result = new ProcessInputResponse();
//
//		Identity tokenPretender = getTokenPrentender(configuration);
//		
//		TokenWrapper token = getAdapterToken(adapterSession, tokenPretender, input);
//		token.setCurrentOwner(owner);
//		
//		result.setToken(token);
//		String response = null;
//		
//		String inputXML = inputMarshaller.marshall(input);
//		
//		OperationManager.getInstance().enter("Adapter Call", new Object[]{token, configuration});
//		try {
//			response = callAdapter(token, AdapterGridIOConstants.PROCESS_INPUT_CMD, inputXML);
//			
//			Output output = outputMarshaller.unmarshall(response);
//			
//			result.setOutput(output);
//			token.getAttributes().put(SELECTION_CRITERION_THREAD, Long.toString(Thread.currentThread().getId()));
//		} catch(IOException  e) {
//			adapterGrid.invalidateToken(token);
//			throw e;
//		} finally {
//			OperationManager.getInstance().exit();
//			token.setCurrentOwner(null);
//			returnAdapterToken(adapterSession, token);						
//		}
//
//		return result;
//	}
//
//	public ProcessInputResponse processInput(GridSession adapterSession, Keyword configuration, Input input) throws Exception {		
//		return processInput(adapterSession, configuration, input, null);
//	}
//	
//	private List<String> getSelectionAttributes() {
//		String attributesStr = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsString("tec.adapters.selectionattributes");
//		return Arrays.asList(attributesStr.split(";"));
//	}
//	
//	private List<String> getSelectionCriteria() {
//		String attributesStr = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsString("tec.adapters.selectioncriteria");
//		return Arrays.asList(attributesStr.split(";"));
//	}
//	
//	
//	private Identity getTokenPrentender(Keyword configuration) {
//		List<String> selectionAttributes = getSelectionAttributes();
//		
//		final Map<String, String> attributes = new HashMap<>();
//		for(String selectionAttribute:selectionAttributes) {
//			String value = null;
//			try {
//				value = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsString(selectionAttribute);
//			} catch (UndefinedVariableException e) {
//				for(Entry<String, String> entry:configuration.getAttributes().entrySet()) {
//					if(entry.getKey().equals(selectionAttribute)) {
//						value = entry.getValue();
//					}
//				}
//			}
//			
//			if(value!=null) {
//				attributes.put(selectionAttribute, value);									
//			}
//		}
//		
//		HashMap<String, Interest> interests = new HashMap<>();
//		interests.put(SELECTION_CRITERION_THREAD,new Interest(Pattern.compile(Long.toString(Thread.currentThread().getId())), false));		
//		
//		List<String> selectionCriteria = getSelectionCriteria();
//		for(String selectionCriterion:selectionCriteria) {
//			String value = null;
//			try {
//				value = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsString(selectionCriterion);
//			} catch (UndefinedVariableException e) {
//				for(Entry<String, String> entry:configuration.getSelectionPatterns().entrySet()) {
//					if(entry.getKey().equals(selectionCriterion)) {
//						value = entry.getValue();
//					}
//				}
//			}
//			
//			if(value!=null) {
//				interests.put(selectionCriterion, new Interest(Pattern.compile(value), true));									
//			}
//		}
//		
//		TokenPretender pretender = new TokenPretender(attributes, interests);
//		return pretender;
//	}
//	
//	
//	public class ProcessInputResponse {
//		
//		Output output;
//		
//		TokenWrapper token;
//
//		public Output getOutput() {
//			return output;
//		}
//
//		public void setOutput(Output output) {
//			this.output = output;
//		}
//
//		public TokenWrapper getToken() {
//			return token;
//		}
//
//		public void setToken(TokenWrapper token) {
//			this.token = token;
//		} 
//	}
//
//	public void releaseSession(GridSession adapterSession) {
//		if(adapterSession!=null) {
//			for(TokenWrapper token:adapterSession.getAllTokens()) {
//				returnAdapterTokenToRegister(token);
//			}
//		}
//	}
//	
//	private TokenWrapper getAdapterToken(GridSession adapterSession, Identity tokenPretender, Input input) {
//		TokenWrapper token = null;
//		if(adapterSession != null) {
//			token = adapterSession.getToken(tokenPretender);
//			if(token == null) {
//				token = getAdapterTokenFromRegister(tokenPretender);
//				adapterSession.putToken(tokenPretender, token);
//			}
//		} else {
//			token = getAdapterTokenFromRegister(tokenPretender);
//		}
//		return token;
//	}
//	
//	private static String NOMATCH_TIMEOUT_PARAMETER = "tec.adapters.borrowtimeout.nomatch.ms";
//	private static String MATCH_EXISTS_TIMEOUT_PARAMETER = "tec.adapters.borrowtimeout.matchexists.ms";
//	private TokenWrapper getAdapterTokenFromRegister(final Identity tokenPretender) {
//		Long matchExistsTimeout = Long.parseLong((String)ExecutionContext.getCurrentContext().getVariablesManager().getVariable(MATCH_EXISTS_TIMEOUT_PARAMETER));
//		Long noMatchExistsTimeout = Long.parseLong((String)ExecutionContext.getCurrentContext().getVariablesManager().getVariable(NOMATCH_TIMEOUT_PARAMETER));
//		
//		OperationManager.getInstance().enter("Token selection", tokenPretender);
//		try {
//			TokenWrapper adapterToken = null;
//			try {
//				adapterToken = adapterGrid.selectToken(tokenPretender, matchExistsTimeout, noMatchExistsTimeout);
//			} catch (TimeoutException e) {
//				String desc = "[attributes=" + tokenPretender.getAttributes() + ", selectionCriteria=" + tokenPretender.getInterests() + "]";
//				throw new RuntimeException("Not able to find any available adapter matching " + desc);
//			} catch (InterruptedException e) {
//				throw new RuntimeException(e);
//			}
//			return adapterToken;
//		} finally {
//			OperationManager.getInstance().exit();
//		}		
//	}
//	
//	private void returnAdapterToken(GridSession adapterSession, TokenWrapper adapterToken) {
//		if(adapterSession==null) {
//			returnAdapterTokenToRegister(adapterToken);
//		}
//	}
//
//	private void returnAdapterTokenToRegister(TokenWrapper adapterToken) {
//		adapterGrid.returnToken(adapterToken);		
//	}
//	
//	private static final String CALLTIMEOUT_PARAM = "tec.adapters.calltimeout.ms";
//	private static final String CONNECTIONTIMEOUT_PARAM = "tec.adapters.connectiontimeout.ms";
//	
//	private String callAdapter(TokenWrapper adapterToken, String adapterCmd, String input) throws Exception {
//		String url = adapterToken.getUrl() + adapterCmd;
//		logger.debug("Calling adapter " + adapterToken.toString() + ". Cmd: " + adapterCmd);
//		
//		int callTimeout = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsInteger(CALLTIMEOUT_PARAM);
//		int connectionTimeout = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsInteger(CONNECTIONTIMEOUT_PARAM);
//		
//		try {			
//			Form form = new Form();
//			form.param(AdapterGridIOConstants.TOKEN_ID_PARAM, adapterToken.getID());       
//			if(input!=null) {
//				form.param(AdapterGridIOConstants.INPUT_PARAM, input);
//			}
//			
//			String response = client.target(url).request().property(ClientProperties.READ_TIMEOUT, callTimeout)
//						.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE.withCharset("UTF-8")), String.class);
//				
//			return response;
////		} catch (ConnectTimeoutException e) {
////			throw new Exception("Timeout occurred while establishing connection to adapter " + adapterToken.getUrl() + ". The timeout is currently set to "  + CONNECTIONTIMEOUT_PARAM + "=" + connectionTimeout +"ms.");
//		} catch (ProcessingException e) {
//			if(e.getCause()!=null&&e.getCause() instanceof SocketTimeoutException) {
//				throw new Exception("Timeout occurred while calling adapter " + adapterToken.getUrl() + ". The timeout is currently set to "  + CALLTIMEOUT_PARAM + "=" + callTimeout +"ms.");
//			} else {
//				throw e;
//			}
//		} catch (Exception e) {
//			throw e;
//		}
//	}
	
	public void close() {
		client.close();
	}
}
