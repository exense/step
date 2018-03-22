package step.core.deployment;

import javax.ws.rs.core.HttpHeaders;

import step.core.GlobalContext;

public interface HttpAuthenticator {
	
	public void init(GlobalContext context);
	public String extractUsername(String request, HttpHeaders headers);
	public boolean authenticate(String request, HttpHeaders headers);
	    
}
