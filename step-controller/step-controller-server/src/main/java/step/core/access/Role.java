package step.core.access;

import java.util.ArrayList;
import java.util.List;

import step.core.accessors.AbstractOrganizableObject;

public class Role extends AbstractOrganizableObject {

	private List<String> rights = new ArrayList<>();

	public List<String> getRights() {
		return rights;
	}

	public void setRights(List<String> rights) {
		this.rights = rights;
	}
}
