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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

import step.core.accessors.Accessor;

public interface ReportNodeAccessor extends Accessor<ReportNode>, ReportTreeAccessor {

	void createIndexesIfNeeded(Long ttl);

	List<ReportNode> getReportNodePath(ObjectId id);

	Iterator<ReportNode> getChildren(ObjectId parentID);

	Iterator<ReportNode> getChildren(ObjectId parentID, int skip, int limit);

	/**
	 * Warning: this method must be used within a try-with-resources statement or similar control structure to ensure that the stream's I/O resources are closed promptly after the stream's operations have completed.
	 * @param executionID the id of the execution
	 * @return a {@link Stream} with all report nodes of this execution
	 */
	Stream<ReportNode> getReportNodesByExecutionID(String executionID);

	/**
	 * Warning: this method must be used within a try-with-resources statement or similar control structure to ensure that the stream's I/O resources are closed promptly after the stream's operations have completed.
	 * @param executionID the id of the execution
	 * @param class_ the _class of the report node
	 * @return a {@link Stream} with all report nodes of this execution with type class_
	 */
	Stream<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_);

	/**
	 * Warning: this method must be used within a try-with-resources statement or similar control structure to ensure that the stream's I/O resources are closed promptly after the stream's operations have completed.
	 * @param executionID the id of the execution
	 * @param customAttributes filter on customer attributes
	 * @return a {@link Stream} with all report nodes of this execution matching provided custom attributes
	 */
	Stream<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID,
			Map<String, String> customAttributes);

	ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID);

	ReportNode getRootReportNode(String executionID);

	Iterator<ReportNode> getChildren(String parentID);

	void removeNodesByExecutionID(String executionID);
}
