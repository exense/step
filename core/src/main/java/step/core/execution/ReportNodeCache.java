package step.core.execution;

import java.util.concurrent.ConcurrentHashMap;

import step.core.artefacts.reports.ReportNode;

public class ReportNodeCache {
	
	private ConcurrentHashMap<String,ReportNode> cache = new ConcurrentHashMap<>();
	
	public void remove(ReportNode node) {
		cache.remove(node.getId().toString());
	}
	
	public ReportNode get(String nodeId) {
		return cache.get(nodeId);
	}
	
	public void put(ReportNode node) {
		cache.put(node.getId().toString(), node);
	}


}
