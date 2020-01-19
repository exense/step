package step.core.access;

import step.core.deployment.Session;

public interface AccessManager {

	public void setRoleResolver(RoleResolver roleResolver);
	
	public Role getRoleInContext(Session session);
	
	public boolean checkRightInContext(Session session, String right);
	
}
