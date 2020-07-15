package step.core.access;

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
		
		//For compatibility with external user management
		if(user == null) {
			user = new ExternalUser();
			user.setUsername(session.getUser().getUsername());
		}
		
		return user.getRole();
	}

}
