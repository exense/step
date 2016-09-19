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
}
