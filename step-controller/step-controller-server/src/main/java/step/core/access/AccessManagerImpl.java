package step.core.access;

import java.util.NoSuchElementException;

import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.Session;

public class AccessManagerImpl implements AccessManager {

	private final RoleProvider roleProvider;
	private RoleResolver roleResolver;
	
	public AccessManagerImpl(RoleProvider roleProvider, RoleResolver roleResolver) {
		super();
		this.roleProvider = roleProvider;
		this.roleResolver = roleResolver;
	}

	@Override
	public void setRoleResolver(RoleResolver roleResolver) {
		this.roleResolver = roleResolver;
	}

	@Override
	public boolean checkRightInContext(Session session, String right) {
		Role role = getRoleInContext(session);
		return role.getRights().contains(right);
	}

	@Override
	public Role getRoleInContext(Session session) {
		String roleName = roleResolver.getRoleInContext(session);
		try {
			Role role = roleProvider.getRoles().stream().filter(r->roleName.equals(r.getAttributes().get(AbstractOrganizableObject.NAME))).findFirst().get();
			return role;
		} catch (NoSuchElementException e) {
			throw new RuntimeException("The role "+roleName+" doesn't exist");
		}
	}
}
