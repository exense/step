package step.core.access;

import java.util.List;

import step.core.GlobalContext;

public interface AccessManager {

	public void init(GlobalContext context);
	
	public List<String> getRights(String username);
	
	public String getRole(String username);
	
	public List<String> getRoles();
}
