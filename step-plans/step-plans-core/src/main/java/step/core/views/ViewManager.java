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
package step.core.views;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.reports.ReportNode;
import step.core.scanner.CachedAnnotationScanner;

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
		Set<Class<?>> viewClasses = CachedAnnotationScanner.getClassesWithAnnotation(View.class);
		
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
				// model might be null if the execution was never started; for various reasons,
				// the "ended" hook will still be executed in that case.
				if (model != null) {
					accessor.save(model);
				}
			} catch(Exception e) {
				logger.error("Error while saving view "+view.getViewId(), e);
			}
		}
	}
	
	public void afterReportNodeSkeletonCreation(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.afterReportNodeSkeletonCreation(model, node));
	}

	public void beforeReportNodeExecution(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.beforeReportNodeExecution(model, node));
	}

	public void afterReportNodeExecution(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.afterReportNodeExecution(model, node));
	}
	
	public void onReportNodeRemoval(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.onReportNodeRemoval(model, node));
	}

	public void onErrorContributionRemoval(ReportNode node) {
		invokeViewHooks(node, (model, view)->view.onErrorContributionRemoval(model, node));
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
