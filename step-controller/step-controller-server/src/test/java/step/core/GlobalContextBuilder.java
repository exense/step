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
package step.core;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.core.access.InMemoryUserAccessor;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.InMemoryExecutionAccessor;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plugins.ControllerPluginManager;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.engine.execution.ExecutionManagerImpl;
import step.expressions.ExpressionHandler;
import step.framework.server.ServerPluginManager;
import step.framework.server.tables.TableRegistry;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.resources.*;

import java.io.File;
import java.io.IOException;

public class GlobalContextBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(GlobalContextBuilder.class);

	public static GlobalContext createGlobalContext() throws CircularDependencyException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		GlobalContext context = new GlobalContext();

		ExpressionHandler expressionHandler = new ExpressionHandler();
		context.setExpressionHandler(expressionHandler);
		DynamicBeanResolver dynamicBeanResolver = new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler()));
		context.setDynamicBeanResolver(dynamicBeanResolver);

		//new since SED-440, class need a full refactoring
		context.setExpressionHandler(expressionHandler);
		context.setDynamicBeanResolver(dynamicBeanResolver);
		
		Configuration configuration = new Configuration();
		ControllerPluginManager pluginManager = new ControllerPluginManager(new ServerPluginManager(configuration));
		context.setContorllerPluginManager(pluginManager);
		
		context.setConfiguration(configuration);
		
		context.put(TableRegistry.class, new TableRegistry());
		InMemoryExecutionAccessor executionAccessor = new InMemoryExecutionAccessor();
		context.setExecutionAccessor(executionAccessor);
		context.setExecutionManager(new ExecutionManagerImpl(executionAccessor));
		context.setPlanAccessor(new InMemoryPlanAccessor());
		context.setReportNodeAccessor(new InMemoryReportNodeAccessor());
		context.setScheduleAccessor(new InMemoryExecutionTaskAccessor());
		context.setUserAccessor(new InMemoryUserAccessor());
		context.setRepositoryObjectManager(new RepositoryObjectManager());
		
		FunctionAccessor functionAccessor = new InMemoryFunctionAccessorImpl();
		context.put(FunctionAccessor.class, functionAccessor);
		
		ResourceAccessor resourceAccessor = new InMemoryResourceAccessor();
		InMemoryResourceRevisionAccessor resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		try {
			File rootFolder = FileHelper.createTempFolder();
			ResourceManager resourceManager = new ResourceManagerImpl(rootFolder,resourceAccessor, resourceRevisionAccessor);
			FileResolver fileResolver = new FileResolver(resourceManager);
			context.put(ResourceAccessor.class, resourceAccessor);
			context.put(ResourceManager.class, resourceManager);
			context.put(FileResolver.class, fileResolver);

			//new since SED-440, class need a full refactoring
			context.setResourceAccessor(resourceAccessor);
			context.setResourceManager(resourceManager);
			context.setFileResolver(fileResolver);
		} catch (IOException e) {
			logger.error("Unable to create temp folder for the resource manager", e);
		}
		
		context.setEntityManager(new EntityManager());
		context.getEntityManager()
				.register(new Entity<Execution, ExecutionAccessor>(EntityManager.executions,
						context.getExecutionAccessor(), Execution.class))
				.register(new Entity<Plan, PlanAccessor>(EntityManager.plans, context.getPlanAccessor(), Plan.class))
				.register(new Entity<ReportNode, ReportNodeAccessor>(EntityManager.reports, context.getReportAccessor(),
						ReportNode.class))
				.register(new Entity<ExecutiontTaskParameters, ExecutionTaskAccessor>(EntityManager.tasks,
						context.getScheduleAccessor(), ExecutiontTaskParameters.class))
				.register(new Entity<User, UserAccessor>(EntityManager.users, context.getUserAccessor(), User.class))
				.register(new Entity<Function, FunctionAccessor>(EntityManager.functions,
						(FunctionAccessor) functionAccessor, Function.class))
				.register(new Entity<Resource, ResourceAccessor>(EntityManager.resources, resourceAccessor,
						Resource.class))
				.register(new Entity<ResourceRevision, ResourceRevisionAccessor>(EntityManager.resourceRevisions,
						resourceRevisionAccessor, ResourceRevision.class));
		
		return context;
	}
}
