/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.core.repositories;

import java.util.Map;

public class RepositoryObjectReference {

	String repositoryID;
	
	Map<String, String> repositoryParameters;
	
	public static final String PLAN_ID = "planid";

	public RepositoryObjectReference() {
		super();
	}

	public RepositoryObjectReference(String repositoryID,
			Map<String, String> repositoryParameters) {
		super();
		this.repositoryID = repositoryID;
		this.repositoryParameters = repositoryParameters;
	}

	public String getRepositoryID() {
		return repositoryID;
	}

	public void setRepositoryID(String repositoryID) {
		this.repositoryID = repositoryID;
	}

	public Map<String, String> getRepositoryParameters() {
		return repositoryParameters;
	}

	public void setRepositoryParameters(Map<String, String> repositoryParameters) {
		this.repositoryParameters = repositoryParameters;
	}

	@Override
	public String toString() {
		return "RepositoryObjectReference [repositoryID=" + repositoryID
				+ ", repositoryParameters=" + repositoryParameters + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((repositoryID == null) ? 0 : repositoryID.hashCode());
		result = prime * result + ((repositoryParameters == null) ? 0 : repositoryParameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryObjectReference other = (RepositoryObjectReference) obj;
		if (repositoryID == null) {
			if (other.repositoryID != null)
				return false;
		} else if (!repositoryID.equals(other.repositoryID))
			return false;
		if (repositoryParameters == null) {
			if (other.repositoryParameters != null)
				return false;
		} else if (!repositoryParameters.equals(other.repositoryParameters))
			return false;
		return true;
	}
}
