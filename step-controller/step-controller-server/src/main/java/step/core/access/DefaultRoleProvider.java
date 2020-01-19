package step.core.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import step.core.accessors.AbstractOrganizableObject;

public class DefaultRoleProvider implements RoleProvider {

	public static Role DEFAULT_ROLE;
	private static List<Role> DEFAULT_ROLES = new ArrayList<Role>();

	{
		DEFAULT_ROLE = new Role();
		DEFAULT_ROLE.addAttribute(AbstractOrganizableObject.NAME, "admin");
		DEFAULT_ROLE.setRights(Arrays.asList(new String[]{"interactive","plan-read","plan-write","plan-delete","plan-execute","kw-read","kw-write","kw-delete","kw-execute","report-read","execution-write","execution-delete","user-write","user-read","task-read","task-write","task-delete","admin","param-read","param-write","param-delete","param-global-write","token-manage"}));
		
		DEFAULT_ROLES.add(DEFAULT_ROLE);
	}

	public DefaultRoleProvider() {
		super();
	}

	@Override
	public List<Role> getRoles() {
		return DEFAULT_ROLES;
	}
}
