package step.core.accessors;

import java.util.Date;

/**
 * This class extends {@link AbstractOrganizableObject} and is used
 * as parent class for all the objects for which modifications should be tracked
 *
 */
public class AbstractTrackedObject extends AbstractOrganizableObject {
	
	public Date lastModificationDate;
	public String lastModificationUser;
	
	public AbstractTrackedObject() {
		super();
	}

	public Date getLastModificationDate() {
		return lastModificationDate;
	}

	public void setLastModificationDate(Date lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	public String getLastModificationUser() {
		return lastModificationUser;
	}

	public void setLastModificationUser(String lastModificationUser) {
		this.lastModificationUser = lastModificationUser;
	}
}
