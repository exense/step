package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.parameter.Parameter;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RemotePlanAccessor extends AbstractRemoteCRUDAccessorImpl<Plan> implements PlanAccessor {

	public RemotePlanAccessor(ControllerCredentials credentials, String path, Class<Plan> entityClass) {
		super(credentials, path, entityClass);
	}

	public RemotePlanAccessor(String path, Class<Plan> entityClass) {
		super(path, entityClass);
	}

	@Override
	public List<Plan> getRange(int skip, int limit) {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("skip", Integer.toString(skip));
		queryParams.put("limit", Integer.toString(limit));
		GenericType<List<Plan>> genericEntity = new GenericType<List<Plan>>(
				parameterizedGenericType) {
		};
		Invocation.Builder b = requestBuilder(path+"all", queryParams);
		return executeRequest(()->b.get(genericEntity));
	}
}
