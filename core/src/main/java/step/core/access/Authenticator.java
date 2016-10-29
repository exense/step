package step.core.access;

import step.core.GlobalContext;

public interface Authenticator {

	public void init(GlobalContext context);
	
	public boolean authenticate(Credentials credentials);
	
	public String getRole(String username);
}
