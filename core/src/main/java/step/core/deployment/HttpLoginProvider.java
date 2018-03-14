package step.core.deployment;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import step.core.GlobalContext;

public interface HttpLoginProvider {
	
	public void init(GlobalContext context, AccessServices accessServices);
	public Response doLogin(String request, HttpHeaders headers);
	Session getSession(ContainerRequestContext crc, String request, HttpHeaders headers);
	    
}
