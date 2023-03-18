package step.controller.multitenancy.model;

public class ProjectMembership {

	private String userId;;
	
	private String roleInProject;
	
	public ProjectMembership() {
		super();
	}

	public ProjectMembership(String userId, String roleInProject) {
		super();
		this.userId = userId;
		this.roleInProject = roleInProject;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getRoleInProject() {
		return roleInProject;
	}

	public void setRoleInProject(String roleInProject) {
		this.roleInProject = roleInProject;
	}
}
