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
package step.core.execution;

import step.attachments.AttachmentManager;
import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.expressions.ExpressionHandler;

public class ExecutionTestHelper {

	public static ExecutionContext setupContext() {
		ExecutionContext c = createContext();
		
		c.getVariablesManager().putVariable(c.getReport(), ReportNodeAttachmentManager.QUOTA_VARNAME, 100);
		c.getVariablesManager().putVariable(c.getReport(), ArtefactHandler.CONTINUE_EXECUTION, "false");
		
		c.associateThread();
		return c;
		
	}
	
	public static ExecutionContext createContext() {
		
		GlobalContext g = createGlobalContext();
		
		ExecutionContext c = createContext(g);

		return c;
	}

	public static ExecutionContext createContext(GlobalContext g) {
		ReportNode root = new ReportNode();
		ExecutionContext c = new ExecutionContext("");
		c.setGlobalContext(g);
		c.getReportNodeCache().put(root);
		c.setReport(root);
		c.setCurrentReportNode(root);
		c.setExecutionParameters(new ExecutionParameters("dummy", null, ExecutionMode.RUN));
		return c;
	}
	
	public static GlobalContext createGlobalContext() {
		GlobalContext context = new GlobalContext();

		context.setExpressionHandler(new ExpressionHandler());
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		PluginManager pluginManager = new PluginManager();
		context.setPluginManager(pluginManager);
		
		context.setConfiguration(Configuration.getInstance());
		
		context.setExecutionAccessor(new InMemoryExecutionAccessor());
		context.setArtefactAccessor(new InMemoryArtefactAccessor());
		context.setReportAccessor(new InMemoryReportNodeAccessor());
		context.setScheduleAccessor(new InMemoryExecutionTaskAccessor());
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getArtefactAccessor()));
		
		context.setEventManager(new EventManager());
		context.setAttachmentManager(new AttachmentManager(Configuration.getInstance()));

		
		return context;
	}
}
