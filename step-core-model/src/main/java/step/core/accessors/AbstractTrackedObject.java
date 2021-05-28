/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
