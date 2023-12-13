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
package step.core.artefacts.reports;

import java.util.Iterator;
import java.util.stream.Stream;

public interface ReportTreeAccessor {

	/**
	 * Get a ReportNode by ID
	 * 
	 * @param id the id of the ReportNode
	 * @return the report node or null if no ReportNode exists with this id
	 */
	public ReportNode get(String id);
	
	/**
	 * Returns the list of children of the ReportNode  
	 * 
	 * @param parentID the ID of the parent ReportNode
	 * @return an Iterator of the list of children
	 */
	public Iterator<ReportNode> getChildren(String parentID);

	Stream<ReportNode> getReportNodesWithContributingErrors(String executionId);
}
