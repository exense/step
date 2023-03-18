package step.controller.multitenancy.model;

import step.core.accessors.AbstractOrganizableObject;

import java.util.List;

public class Project extends AbstractOrganizableObject{

	public final static String entityName = "projects";

	private boolean global;
	
	private List<ProjectMembership> members;

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public List<ProjectMembership> getMembers() {
		return members;
	}

	public void setMembers(List<ProjectMembership> members) {
		this.members = members;
	}
}
