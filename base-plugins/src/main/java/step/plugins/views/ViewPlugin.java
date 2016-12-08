package step.plugins.views;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ViewPlugin extends AbstractPlugin {

	public static final String VIEW_PLUGIN_KEY = "ViewPlugin_Instance";
	
	private final ConcurrentHashMap<String, View<ViewModel>> register = new ConcurrentHashMap<>();
	
	private ViewModelAccessor accessor;
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		loadViews();
		
		accessor = new ViewModelAccessor(context.getMongoClient(), context.getMongoDatabase());
		
		context.getServiceRegistrationCallback().registerService(ViewPluginServices.class);
		context.put(VIEW_PLUGIN_KEY, this);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadViews() {
		Set<Class<? extends View>> viewClasses = new Reflections("step").getSubTypesOf(View.class);
		
		for(Class<? extends View> viewClass:viewClasses) {
			View view;
			try {
				view = viewClass.newInstance();
				register(view.getViewId(), view);
			} catch (InstantiationException | IllegalAccessException e) {
				
			}
		}
	}
	
	public void register(String viewId, View<ViewModel> view) {
		register.put(viewId, view);
	}
	
	public ViewModel query(String viewId, String executionId) {
		View<?> view =	register.get(viewId);
		if(view!=null) {
			ViewModel model = view.getModel(executionId);
			if(model==null) {
				model = accessor.get(viewId, executionId, ViewModel.class);
				if(model!=null) {
					return model;
				} else {
					throw new RuntimeException("Unable to find model for view '"+viewId+"' and execution: "+executionId);
				}
			} else {
				return model;
			}
		} else {
			throw new RuntimeException("Invalid view id: "+ viewId);
		}
	}

	@Override
	public void executionStart(ExecutionContext context) {
		for(View<ViewModel> view:register.values()) {
			ViewModel model = view.init();
			model.setExecutionId(context.getExecutionId());
			model.setViewId(view.getViewId());
			view.addModel(context.getExecutionId(), model);
		}
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
		for(View<?> view:register.values()) {
			ViewModel model = view.removeModel(context.getExecutionId());
			accessor.save(model);
		}
	}

	@Override
	public void afterReportNodeSkeletonCreation(ReportNode node) {
		for(View<ViewModel> view:register.values()) {
			ViewModel model = view.getModel(node.getExecutionID());
			synchronized (model) {
				view.afterReportNodeSkeletonCreation(model, node);				
			}
		}
	}

	@Override
	public void afterReportNodeExecution(ReportNode node) {
		for(View<ViewModel> view:register.values()) {
			ViewModel model = view.getModel(node.getExecutionID());
			synchronized (model) {
				view.afterReportNodeExecution(model, node);
			}
		}
	}
	
}
