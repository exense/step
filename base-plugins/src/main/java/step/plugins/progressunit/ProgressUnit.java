
package step.plugins.progressunit;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.reports.ReportNode;

public class ProgressUnit {

	static Logger logger = LoggerFactory.getLogger(ProgressUnit.class); 
	
	private Map<String, ProgressView> activeViews = new HashMap<>();
	
	public void addProgressView(String viewClassName) {
		ProgressView view = initializeView(viewClassName);
		activeViews.put(viewClassName, view);
	}
	
	private ProgressView initializeView(String viewClassName) {
		ProgressView view;
		try {
			@SuppressWarnings("unchecked")
			Class<ProgressView> viewClass = (Class<ProgressView>) Class.forName(viewClassName);
			view = viewClass.newInstance();
			return view;
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			logger.error("Unable to instantiate view " + viewClassName, e);
			throw new RuntimeException(e);
		}
	}
	
	public int getMaxProgress(String viewClassName) {
		ProgressView view = activeViews.get(viewClassName);
		return view.getMaxProgress();
	}
	
	public void afterReportNodeSkeletonCreation(ReportNode node) {
		for(ProgressView view:activeViews.values()) {
			view.skeletonReportNodeCreated(node);
		}
	}
	
	public void afterReportNodeExecution(ReportNode node) {
		for(ProgressView view:activeViews.values()) {
			view.reportNodeExecuted(node);
		}
	}
	
}
