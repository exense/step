package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.parameter.Parameter;
import step.parameter.ParameterAccessor;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RemoteParameterAccessor extends AbstractRemoteCRUDAccessorImpl<Parameter> implements ParameterAccessor {

	public RemoteParameterAccessor(ControllerCredentials credentials, String path, Class<Parameter> entityClass) {
		super(credentials, path, entityClass);
	}

	public RemoteParameterAccessor(String path, Class<Parameter> entityClass) {
		super(path, entityClass);
	}

	@Override
	public List<Parameter> getRange(int skip, int limit) {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("skip", Integer.toString(skip));
		queryParams.put("limit", Integer.toString(limit));
		GenericType<List<Parameter>> genericEntity = new GenericType<List<Parameter>>(
				parameterizedGenericType) {
		};
		Invocation.Builder b = requestBuilder(path+"all", queryParams);
		return executeRequest(()->b.get(genericEntity));
	}
}
