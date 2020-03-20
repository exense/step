package step.plugins.views;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ViewPlugin extends AbstractControllerPlugin {
	
	private static Logger logger = LoggerFactory.getLogger(ViewPlugin.class);

	public static final String VIEW_PLUGIN_KEY = "ViewPlugin_Instance";
	
	private final ConcurrentHashMap<String, AbstractView<ViewModel>> register = new ConcurrentHashMap<>();
	
	private ViewModelAccessor accessor;
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		loadViews();
		
		accessor = new ViewModelAccessorImpl(context.getMongoClientSession());
		
		context.put(ViewModelAccessor.class, accessor);
		context.getServiceRegistrationCallback().registerService(ViewPluginServices.class);
		context.put(VIEW_PLUGIN_KEY, this);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadViews() {
		Set<Class<?>> viewClasses = new Reflections("step").getTypesAnnotatedWith(View.class);
		
		for(Class<?> viewClass:viewClasses) {
			AbstractView view;
			try {
				view = (AbstractView) viewClass.newInstance();
				register(view.getViewId(), view);
			} catch (InstantiationException | IllegalAccessException e) {
				
			}
		}
	}
	
	public void register(String viewId, AbstractView<ViewModel> view) {
		register.put(viewId, view);
	}
	
	public ViewModel query(String viewId, String executionId) {
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

	@Override
	public void executionStart(ExecutionContext context) {
		for(AbstractView<ViewModel> view:register.values()) {
			try {
				ViewModel model = view.init();
				model.setExecutionId(context.getExecutionId());
				model.setViewId(view.getViewId());
				view.addModel(context.getExecutionId(), model);
			} catch (Exception e) {
				logger.error("Error while initializing view "+view.getViewId(), e);
			}
		}
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
		for(AbstractView<?> view:register.values()) {
			try {
				ViewModel model = view.removeModel(context.getExecutionId());
				accessor.save(model);
			} catch(Exception e) {
				logger.error("Error while saving view "+view.getViewId(), e);
			}
		}
	}

	@Override
	public void afterReportNodeSkeletonCreation(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.afterReportNodeSkeletonCreation(model, node));
	}

	@Override
	public void afterReportNodeExecution(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.afterReportNodeExecution(model, node));
	}
	
	@Override
	public void rollbackReportNode(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.rollbackReportNode(model, node));
	}
	
	protected void invokeViewHooks(ReportNode node, BiConsumer<ViewModel, AbstractView<ViewModel>> consumer) {
		for(AbstractView<ViewModel> view:register.values()) {
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
	
}
