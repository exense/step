package step.plugins.views;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.reports.ReportNode;
import step.core.scanner.AnnotationScanner;

public class ViewManager {

	private static final Logger logger = LoggerFactory.getLogger(ViewManager.class);
	
	private final ConcurrentHashMap<String, AbstractView<ViewModel>> register = new ConcurrentHashMap<>();
	private ViewModelAccessor accessor;
	
	public ViewManager(ViewModelAccessor accessor) {
		super();
		this.accessor = accessor;
		loadViews();
	}

	public Collection<AbstractView<ViewModel>> getViews() {
		return register.values();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadViews() {
		Set<Class<?>> viewClasses = AnnotationScanner.getClassesWithAnnotation(View.class);
		
		for(Class<?> viewClass:viewClasses) {
			AbstractView view;
			try {
				view = (AbstractView) viewClass.newInstance();
				registerView(view.getViewId(), view);
			} catch (InstantiationException | IllegalAccessException e) {
				logger.error("Error while loading view "+viewClass.toString(), e);
			}
		}
	}
	
	public void registerView(String viewId, AbstractView<ViewModel> view) {
		register.put(viewId, view);
	}
	
	public void createViewModelsForExecution(String executionId) {
		for(AbstractView<ViewModel> view:getViews()) {
			try {
				ViewModel model = view.init();
				model.setExecutionId(executionId);
				model.setViewId(view.getViewId());
				view.addModel(executionId, model);
			} catch (Exception e) {
				logger.error("Error while initializing view "+view.getViewId(), e);
			}
		}
	}

	public void closeViewModelsForExecution(String executionId) {
		for(AbstractView<?> view:getViews()) {
			try {
				ViewModel model = view.removeModel(executionId);
				accessor.save(model);
			} catch(Exception e) {
				logger.error("Error while saving view "+view.getViewId(), e);
			}
		}
	}
	
	public void afterReportNodeSkeletonCreation(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.afterReportNodeSkeletonCreation(model, node));
	}

	public void afterReportNodeExecution(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.afterReportNodeExecution(model, node));
	}
	
	public void rollbackReportNode(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.rollbackReportNode(model, node));
	}
	
	private void invokeViewHooks(ReportNode node, BiConsumer<ViewModel, AbstractView<ViewModel>> consumer) {
		for(AbstractView<ViewModel> view:getViews()) {
			ViewModel model = view.getModel(node.getExecutionID());
			synchronized (model) {
				try {
					consumer.accept(model, view);
				} catch(Exception e) {
					logger.error("Error while invoking view "+view.getViewId()+" for node "+node.toString(), e);
				}
			}
		}
	}
	
	public ViewModel queryView(String viewId, String executionId) {
		AbstractView<?> view =	register.get(viewId);
		if(view!=null) {
			ViewModel model = view.getModel(executionId);
			if(model==null) {
				model = accessor.get(viewId, executionId, ViewModel.class);
				if(model!=null) {
					return model;
				} else {
					// return default view
					return view.init();
				}
			} else {
				return model;
			}
		} else {
			throw new RuntimeException("Invalid view id: "+ viewId);
		}
	}
}
