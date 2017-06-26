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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

public class InMemoryReportNodeAccessor extends ReportNodeAccessor {

	Map<ObjectId, ReportNode> map = new HashMap<>();

	@Override
	public void save(ReportNode node) {
		map.put(node.getId(), node);
	}

	@Override
	public ReportNode get(ObjectId nodeId) {
		return map.get(nodeId);
	}

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

}
