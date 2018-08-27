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
package step.core.artefacts.reports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.commons.datatable.DataTable;
import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryReportNodeAccessor extends InMemoryCRUDAccessor<ReportNode> implements ReportNodeAccessor {

	@Override
	public Iterator<ReportNode> getChildren(ObjectId parentID) {
		return map.values().stream().filter(node->parentID.equals(node.getParentID())).iterator();
	}

	@Override
	public ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID) {
		for(ReportNode node:map.values()) {
			if(parentID.equals(node.getParentID())&&artefactID.equals(node.getArtefactID())) {
				return node;
			}
		}
		return null;

	}

	@Override
	public Iterator<ReportNode> getReportNodesByExecutionIDAndArtefactID(String executionID, String artefactID) {
		List<ReportNode> nodes = new ArrayList<>();
		for(ReportNode node:map.values()) {
			if(executionID.equals(node.getExecutionID())&&node.getArtefactID()!=null&&artefactID.equals(node.getArtefactID().toString())) {
				nodes.add(node);
			}
		}
		return nodes.iterator();
	}

	@Override
	public void createIndexesIfNeeded(Long ttl) {
		
	}

	@Override
	public List<ReportNode> getReportNodePath(ObjectId id) {
		LinkedList<ReportNode> result = new LinkedList<>();
		appendParentNodeToPath(result, get(id));
		return result;
	}
	
	private void appendParentNodeToPath(LinkedList<ReportNode> path, ReportNode node) {
		path.addFirst(node);
		ReportNode parentNode;
		if(node.getParentID()!=null) {
			parentNode = get(node.getParentID());
			if (parentNode != null) {
				appendParentNodeToPath(path, parentNode);
			}
		}
	}

	@Override
	public Iterator<ReportNode> getChildren(ObjectId parentID, int skip, int limit) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterator<ReportNode> getReportNodesByExecutionID(String executionID) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public long countReportNodesByExecutionID(String executionID) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterator<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterator<ReportNode> getLeafReportNodesByExecutionID(String executionID) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterator<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID,
			List<Map<String, String>> customAttributes) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterator<ReportNode> getFailedLeafReportNodesByExecutionID(String executionID) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public DataTable getTimeBasedReport(String executionID, int resolution) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ReportNode getRootReportNode(String executionID) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Map<ReportNodeStatus, Integer> getLeafReportNodesStatusDistribution(String executionID,
			String reportNodeClass) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterator<ReportNode> getChildren(String parentID) {
		return getChildren(new ObjectId(parentID));
	}

}
