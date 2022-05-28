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

import java.io.File;

import ch.exense.commons.app.Configuration;
import com.sun.xml.bind.v2.ContextFactory;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.attachments.FileResolver;
import step.core.access.User;
import step.core.access.UserAccessorImpl;
import step.core.accessors.AbstractAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessorImpl;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.CollectionFactoryConfigurationParser;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.entities.Bean;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessorImpl;
import step.core.plans.Plan;
import step.core.plans.PlanAccessorImpl;
import step.core.plans.PlanEntity;
import step.core.plugins.ControllerPluginManager;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionTaskAccessorImpl;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.tables.AbstractTable;
import step.core.tables.TableRegistry;
import step.core.tasks.AsyncTaskManager;
import step.dashboards.DashboardSession;
import step.engine.execution.ExecutionManagerImpl;
import step.expressions.ExpressionHandler;
import step.framework.server.ServerPluginManager;
import step.framework.server.ServiceRegistrationCallback;
import step.resources.Resource;
import step.resources.ResourceAccessor;
import step.resources.ResourceAccessorImpl;
import step.resources.ResourceEntity;
import step.resources.ResourceImporter;
import step.resources.ResourceManager;
import step.resources.ResourceManagerControllerPlugin;
import step.resources.ResourceManagerImpl;
import step.resources.ResourceRevision;
import step.resources.ResourceRevisionAccessor;
import step.resources.ResourceRevisionAccessorImpl;


public class Controller {

	public static final Version VERSION = new Version(3,20,0);
	private Configuration configuration;
	
	private GlobalContext context;
	
	private ServiceRegistrationCallback serviceRegistrationCallback;
	
	public Controller(GlobalContext context) {
		super();
		this.context = context;
		this.configuration = context.getConfiguration();
		ContextFactory ctx;
	}

	public void init(ServiceRegistrationCallback serviceRegistrationCallback) throws Exception {			
		this.serviceRegistrationCallback = serviceRegistrationCallback;
		
		initContext();
		context.setServiceRegistrationCallback(serviceRegistrationCallback);
	}
	
	private void initContext() throws ClassNotFoundException, PluginManager.Builder.CircularDependencyException, InstantiationException, IllegalAccessException {
		context.setConfiguration(configuration);
		//Set version here for now
		context.put(Version.class, Controller.VERSION);
		
		CollectionFactory collectionFactory = CollectionFactoryConfigurationParser.parseConfiguration(configuration);
		context.setCollectionFactory(collectionFactory);

		context.setContorllerPluginManager(new ControllerPluginManager(context.require(ServerPluginManager.class)));
		
		ResourceAccessor resourceAccessor = new ResourceAccessorImpl(collectionFactory.getCollection("resources", Resource.class));
		ResourceRevisionAccessor resourceRevisionAccessor = new ResourceRevisionAccessorImpl(
				collectionFactory.getCollection("resourceRevisions", ResourceRevision.class));
		String resourceRootDir = ResourceManagerControllerPlugin.getResourceDir(configuration);
		ResourceManager resourceManager = new ResourceManagerImpl(new File(resourceRootDir), resourceAccessor, resourceRevisionAccessor);
		FileResolver fileResolver = new FileResolver(resourceManager);
		
		context.setResourceAccessor(resourceAccessor);
		context.setResourceManager(resourceManager);
		context.setFileResolver(fileResolver);
		
		TableRegistry tableRegistry = new TableRegistry();
		context.put(TableRegistry.class, tableRegistry);		
		ExecutionAccessorImpl executionAccessor = new ExecutionAccessorImpl(
				collectionFactory.getCollection("executions", Execution.class));
		context.setExecutionAccessor(executionAccessor);		
		context.setExecutionManager(new ExecutionManagerImpl(executionAccessor));
		
		context.setPlanAccessor(new PlanAccessorImpl(collectionFactory.getCollection("plans", Plan.class)));
		context.setReportNodeAccessor(
				new ReportNodeAccessorImpl(collectionFactory.getCollection("reports", ReportNode.class)));
		context.setScheduleAccessor(new ExecutionTaskAccessorImpl(
				collectionFactory.getCollection("tasks", ExecutiontTaskParameters.class)));

		Collection<User> userCollection = collectionFactory.getCollection("users", User.class);
		context.setUserAccessor(new UserAccessorImpl(userCollection));
		tableRegistry.register("users", new AbstractTable<User>(userCollection, false));
		
		
		context.setRepositoryObjectManager(new RepositoryObjectManager());
		context.setExpressionHandler(new ExpressionHandler(configuration.getProperty("tec.expressions.scriptbaseclass"), 
				configuration.getPropertyAsInteger("tec.expressions.warningthreshold",200),
				configuration.getPropertyAsInteger("tec.expressions.pool.maxtotal",1000),
				configuration.getPropertyAsInteger("tec.expressions.pool.maxidle",-1)));
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		context.setEntityManager(new EntityManager());
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		PlanLocator planLocator = new PlanLocator(context.getPlanAccessor(), selectorHelper);
		
		EntityManager entityManager = context.getEntityManager();
		entityManager
				// Bean entity used for remote test
				.register(new Entity<Bean, AbstractAccessor<Bean>>("beans",
						new AbstractAccessor(context.getCollectionFactory().getCollection("beans", Bean.class)),
						Bean.class))
				.register(new Entity<>(EntityManager.executions, context.getExecutionAccessor(), Execution.class))
				.register(new PlanEntity(context.getPlanAccessor(), planLocator, entityManager))
				.register(new Entity<>(EntityManager.reports, context.getReportAccessor(), ReportNode.class))
				.register(new Entity<>(EntityManager.tasks, context.getScheduleAccessor(), ExecutiontTaskParameters.class))
				.register(new Entity<>(EntityManager.users, context.getUserAccessor(), User.class))
				.register(new ResourceEntity(resourceAccessor, resourceManager, fileResolver, entityManager))
				.register(new Entity<>(EntityManager.resourceRevisions, resourceRevisionAccessor, ResourceRevision.class))
				.register(new Entity<>("sessions",
						new AbstractAccessor<>(
								collectionFactory.getCollection("sessions", DashboardSession.class)),
						DashboardSession.class));
		
		entityManager.registerImportHook(new ResourceImporter(context.getResourceManager()));
		entityManager.getEntityByName("sessions").setByPassObjectPredicate(true);

		context.put(AsyncTaskManager.class, new AsyncTaskManager());
		context.put(WebApplicationConfigurationManager.class, new WebApplicationConfigurationManager());

		createOrUpdateIndexes();

	}

	private void createOrUpdateIndexes() {
		long dataTTL = context.getConfiguration().getPropertyAsInteger("db.datattl", 0);
		context.getReportAccessor().createIndexesIfNeeded(dataTTL);
		context.getExecutionAccessor().createIndexesIfNeeded(dataTTL);
	}

	public void destroy() {
		serviceRegistrationCallback.stop();
	}

	
}
