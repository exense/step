package step.core.access;

import step.core.controller.errorhandling.ApplicationException;
import step.core.deployment.Session;

public class RoleResolverImpl implements RoleResolver {

	private final UserAccessor userAccessor;
	
	public RoleResolverImpl(UserAccessor userAccessor) {
		super();
		this.userAccessor = userAccessor;
	}

	@Override
	public String getRoleInContext(Session session) {
		User user = userAccessor.get(session.getUser().getId());
		
		if(user == null) {
			throw new ApplicationException(100, "Unknow user '"+session.getUser()+"': this user is not defined in step", null);
		}
		
		return user.getRole();
	}
}
