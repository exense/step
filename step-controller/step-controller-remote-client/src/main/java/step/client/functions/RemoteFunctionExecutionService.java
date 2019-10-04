package step.client.functions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.services.GetTokenHandleParameter;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
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
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession, TokenWrapperOwner tokenWrapperOwner) {
		GetTokenHandleParameter parameter = new GetTokenHandleParameter();
		parameter.setAttributes(attributes);
		parameter.setInterests(interests);
		parameter.setCreateSession(createSession);
		
		Builder b = requestBuilder("/rest/functions/executor/tokens/select");
		Entity<GetTokenHandleParameter> entity = Entity.entity(parameter, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity,TokenWrapper.class));
	}

	@Override
	public void returnTokenHandle(String tokenId) {
		Builder b = requestBuilder("/rest/functions/executor/tokens/"+tokenId+"/return");
		executeRequest(()->b.post(Entity.json(null)));
	}

	@Override
	public <IN, OUT> Output<OUT> callFunction(String tokenId, Function function, FunctionInput<IN> input,
			Class<OUT> outputClass) {
		Builder b = requestBuilder("/rest/functions/executor/tokens/"+tokenId+"/execute/"+function.getId().toString());
		Entity<FunctionInput<IN>> entity = Entity.entity(input, MediaType.APPLICATION_JSON);
		
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
