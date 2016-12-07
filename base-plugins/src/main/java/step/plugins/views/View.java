package step.plugins.views;

import java.util.concurrent.ConcurrentHashMap;

import step.core.artefacts.reports.ReportNode;


public abstract class View<V extends ViewModel> {

	private ConcurrentHashMap<String, V> models = new ConcurrentHashMap<>();
	
	public abstract V init();
	
	public abstract String getViewId();
	
	public V getModel(String executionId) {
		return models.get(executionId);
	}
	
	public V removeModel(String executionId) {
		return models.remove(executionId);
	}
	
	public void addModel(String executionId, V model) {
		models.put(executionId, model);
	}
	
	public abstract void afterReportNodeSkeletonCreation(V model, ReportNode node);
	
	public abstract void afterReportNodeExecution(V model, ReportNode node);
}
