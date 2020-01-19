package step.core.access;

import step.core.deployment.Session;

public interface RoleResolver {

	public String getRoleInContext(Session session);
}
