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
package step.core.plugins;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public abstract class AbstractPlugin implements PluginCallbacks {

	@Override
	public void executionControllerStart(GlobalContext context)  throws Exception {}

	@Override
	public void executionControllerDestroy(GlobalContext context) {}

	@Override
	public void executionStart(ExecutionContext context) {}

	@Override
	public void beforeExecutionEnd(ExecutionContext context) {}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {}

	@Deprecated()
	public void afterReportNodeSkeletonCreation(ReportNode node) {
	}

	@Deprecated()
	public void beforeReportNodeExecution(ReportNode node) {
	}
	
	@Deprecated()
	public void afterReportNodeExecution(ReportNode node) {
	}
	
	@Override
	public void afterReportNodeSkeletonCreation(ExecutionContext context, ReportNode node) {
		afterReportNodeSkeletonCreation(node);
	}

	@Override
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
		beforeReportNodeExecution(node);
	}
	
	@Override
	public void afterReportNodeExecution(ExecutionContext context, ReportNode node) {
		afterReportNodeExecution(node);
	}

	@Override
	public void associateThread(ExecutionContext context, Thread thread) {}

	@Override
	public void unassociateThread(ExecutionContext context, Thread thread) {}
	
	public boolean validate(GlobalContext context) {
		return true;
	}

	public WebPlugin getWebPlugin() {
		return null;
	}
	
	protected void registerWebapp(GlobalContext context, String path) {
		ResourceHandler bb = new ResourceHandler();
		
		bb.setResourceBase(this.getClass().getResource("webapp").toExternalForm());
		
		ContextHandler ctx = new ContextHandler(path);
		ctx.setHandler(bb);
		
		context.getServiceRegistrationCallback().registerHandler(ctx);
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
