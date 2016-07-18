package step.core.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;

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
