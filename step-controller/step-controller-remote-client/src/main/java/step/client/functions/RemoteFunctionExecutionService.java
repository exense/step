package step.client.functions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.services.CallFunctionInput;
import step.functions.services.GetTokenHandleParameter;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;

public class RemoteFunctionExecutionService extends AbstractRemoteClient implements FunctionExecutionService {

	public RemoteFunctionExecutionService(ControllerCredentials credentials){
		super(credentials);
	}
	
	public RemoteFunctionExecutionService(){
		super();
	}
	
	@Override
	public TokenWrapper getLocalTokenHandle() {
		GetTokenHandleParameter parameter = new GetTokenHandleParameter();
		parameter.setLocal(true);
		
		Builder b = requestBuilder("/rest/functions/executor/tokens/select");
		Entity<GetTokenHandleParameter> entity = Entity.entity(parameter, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity,TokenWrapper.class));
	}

	@Override
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession) {
		GetTokenHandleParameter parameter = new GetTokenHandleParameter();
		parameter.setAttributes(attributes);
		parameter.setInterests(interests);
		parameter.setCreateSession(createSession);
		
		Builder b = requestBuilder("/rest/functions/executor/tokens/select");
		Entity<GetTokenHandleParameter> entity = Entity.entity(parameter, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity,TokenWrapper.class));
	}

	@Override
	public void returnTokenHandle(TokenWrapper adapterToken) {
		Builder b = requestBuilder("/rest/functions/executor/tokens/return");
		Entity<TokenWrapper> entity = Entity.entity(adapterToken, MediaType.APPLICATION_JSON);
		executeRequest(()->b.post(entity,TokenWrapper.class));
	}

	@Override
	public <IN, OUT> Output<OUT> callFunction(TokenWrapper tokenHandle, Function function, Input<IN> input,
			Class<OUT> outputClass) {
		CallFunctionInput i = new CallFunctionInput();
		i.setFunctionId(function.getId().toString());
		i.setInput((Input<JsonObject>) input);
		i.setTokenHandle(tokenHandle);
		
		Builder b = requestBuilder("/rest/functions/executor/execute");
		Entity<CallFunctionInput> entity = Entity.entity(i, MediaType.APPLICATION_JSON);
		
		ParameterizedType parameterizedGenericType = new ParameterizedType() {
	        public Type[] getActualTypeArguments() {
	            return new Type[] { outputClass };
	        }

	        public Type getRawType() {
	            return Output.class;
	        }

	        public Type getOwnerType() {
	            return Output.class;
	        }
	    };

	    GenericType<Output<OUT>> genericType = new GenericType<Output<OUT>>(
	            parameterizedGenericType) {
	    };
		
		return executeRequest(()->b.post(entity,genericType));
	}
}
