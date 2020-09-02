package step.dashboards;

import step.core.accessors.AbstractOrganizableObject;

public class DashboardSession extends AbstractOrganizableObject {
	protected String name;
	protected Object object;

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}



	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
