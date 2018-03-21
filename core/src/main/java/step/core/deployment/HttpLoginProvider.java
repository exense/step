package step.core.deployment;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import step.core.GlobalContext;
import step.core.access.AccessManager;

public interface HttpLoginProvider {
	
	public void init(GlobalContext context, AccessServices accessServices, AccessManager accessManager);
	public Response doLogin(String request, HttpHeaders headers);
	public Object getLoginInformation(Object filterInfo);
	    
}
