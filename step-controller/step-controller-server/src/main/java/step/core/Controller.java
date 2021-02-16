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
import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.artefacts.CallPlan;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.attachments.FileResolver;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.access.UserAccessorImpl;
import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.MongoClientSession;
import step.core.accessors.PlanAccessorImpl;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionRegistry;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeAccessorImpl;
import step.core.controller.ControllerSettingAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionAccessorImpl;
import step.core.execution.model.ExecutionStatus;
import step.core.imports.GenericDBImporter;
import step.core.imports.PlanImporter;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plugins.ControllerInitializationPlugin;
import step.core.plugins.ControllerPlugin;
import step.core.plugins.ControllerPluginManager;
import step.core.plugins.ModuleChecker;
import step.core.plugins.PluginManager;
import step.core.plugins.PluginManager.Builder;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.ExecutionTaskAccessorImpl;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.Executor;
import step.dashboards.DashboardSession;
import step.engine.execution.ExecutionManagerImpl;
import step.expressions.ExpressionHandler;
import step.resources.Resource;
import step.resources.ResourceAccessor;
import step.resources.ResourceAccessorImpl;
import step.resources.ResourceImpoter;
import step.resources.ResourceManager;
import step.resources.ResourceManagerControllerPlugin;
import step.resources.ResourceManagerImpl;
import step.resources.ResourceRevision;
import step.resources.ResourceRevisionAccessor;
import step.resources.ResourceRevisionAccessorImpl;
import step.resources.ResourceRevisionsImporter;

public class Controller {
	
	private static final Logger logger = LoggerFactory.getLogger(Controller.class);
	
	private Configuration configuration;
	
	private GlobalContext context;
		
	private ControllerPluginManager pluginManager;
	
	private ExecutionScheduler scheduler;
	
	private ServiceRegistrationCallback serviceRegistrationCallback;
	
	private MongoClientSession mongoClientSession;
	
	public Controller(Configuration configuration) {
		super();
		this.configuration = configuration;
	}

	public void init(ServiceRegistrationCallback serviceRegistrationCallback) throws Exception {			
		this.serviceRegistrationCallback = serviceRegistrationCallback;
		
		initContext();
		context.setServiceRegistrationCallback(serviceRegistrationCallback);
		
		recover();
		
		ControllerPlugin pluginProxy = pluginManager.getProxy();
		logger.info("Starting controller...");
		pluginProxy.executionControllerStart(context);
		logger.info("Executing migration tasks...");
		pluginProxy.migrateData(context);
		logger.info("Initializing data...");
		pluginProxy.initializeData(context);
		logger.info("Calling post data initialization scripts...");
		pluginProxy.afterInitializeData(context);
			
		scheduler = new ExecutionScheduler(context.require(ControllerSettingAccessor.class), context.getScheduleAccessor(), new Executor(context));
		scheduler.start();
	}
	
	private void initContext() throws Exception {
		context = new GlobalContext();
		context.setConfiguration(configuration);
		
		Builder<ControllerInitializationPlugin> builder = new PluginManager.Builder<ControllerInitializationPlugin>(ControllerInitializationPlugin.class);
		PluginManager<ControllerInitializationPlugin> controllerInitializationPluginManager = builder.withPluginsFromClasspath().build();
		logger.info("Checking preconditions...");
		controllerInitializationPluginManager.getProxy().checkPreconditions(context);
		
		ModuleChecker moduleChecker = context.get(ModuleChecker.class);
		pluginManager = new ControllerPluginManager(configuration, moduleChecker);
		context.setPluginManager(pluginManager);
		
		mongoClientSession = new MongoClientSession(configuration);
		
		ResourceAccessor resourceAccessor = new ResourceAccessorImpl(mongoClientSession);
		ResourceRevisionAccessor resourceRevisionAccessor = new ResourceRevisionAccessorImpl(mongoClientSession);
		String resourceRootDir = ResourceManagerControllerPlugin.getResourceDir(configuration);
		ResourceManager resourceManager = new ResourceManagerImpl(new File(resourceRootDir), resourceAccessor, resourceRevisionAccessor);
		FileResolver fileResolver = new FileResolver(resourceManager);
		
		context.setResourceAccessor(resourceAccessor);
		context.setResourceManager(resourceManager);
		context.setFileResolver(fileResolver);
		
		context.setMongoClientSession(mongoClientSession);
		CollectionRegistry collectionRegistry = new CollectionRegistry();
		context.put(CollectionRegistry.class, collectionRegistry);		
		ExecutionAccessorImpl executionAccessor = new ExecutionAccessorImpl(mongoClientSession);
		context.setExecutionAccessor(executionAccessor);		
		context.setExecutionManager(new ExecutionManagerImpl(executionAccessor));
		context.setPlanAccessor(new PlanAccessorImpl(mongoClientSession));		
		context.setReportNodeAccessor(new ReportNodeAccessorImpl(mongoClientSession));
		context.setScheduleAccessor(new ExecutionTaskAccessorImpl(mongoClientSession));
		context.setUserAccessor(new UserAccessorImpl(mongoClientSession));
		collectionRegistry.register("users", new Collection<User>(mongoClientSession.getMongoDatabase(), "users", User.class, false));
		context.setRepositoryObjectManager(new RepositoryObjectManager());
		context.setExpressionHandler(new ExpressionHandler(configuration.getProperty("tec.expressions.scriptbaseclass"), 
				configuration.getPropertyAsInteger("tec.expressions.warningthreshold"),
				configuration.getPropertyAsInteger("tec.expressions.pool.maxtotal",1000),
				configuration.getPropertyAsInteger("tec.expressions.pool.maxidle",-1)));
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		context.setEntityManager(new EntityManager(context));
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(getContext().getExpressionHandler()));
		SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		PlanLocator planLocator = new PlanLocator(null,getContext().getPlanAccessor(),selectorHelper);
		context.getEntityManager()
			.register( new Entity<Execution, ExecutionAccessor>(
				EntityManager.executions, context.getExecutionAccessor(), Execution.class, 
				new GenericDBImporter<Execution, ExecutionAccessor>(context) {
			}))
			.register(new Entity<Plan,PlanAccessor>(
					EntityManager.plans, context.getPlanAccessor(), Plan.class, new PlanImporter(context)){
				@Override
				public boolean shouldExport(AbstractIdentifiableObject a) {
					return ((Plan) a).isVisible();
				}
				@Override
				public String resolve(Object artefact) {
					if (artefact instanceof CallPlan) {
						return planLocator.selectPlan((CallPlan) artefact).getId().toHexString();
					} else {
						return null;
					}
				}
			})
			.register(new Entity<ReportNode,ReportNodeAccessor>(
					EntityManager.reports, context.getReportAccessor(), ReportNode.class,
					new GenericDBImporter<ReportNode, ReportNodeAccessor>(context)))
			.register(new Entity<ExecutiontTaskParameters,ExecutionTaskAccessor>(
					EntityManager.tasks, context.getScheduleAccessor(), ExecutiontTaskParameters.class, 
					new GenericDBImporter<ExecutiontTaskParameters, ExecutionTaskAccessor>(context)))
			.register(new Entity<User,UserAccessor>(
					EntityManager.users, context.getUserAccessor(), User.class, 
					new GenericDBImporter<User, UserAccessor>(context)))
			.register(new Entity<Resource, ResourceAccessor>(EntityManager.resources, resourceAccessor,
						Resource.class, new ResourceImpoter(context)))
			.register(new Entity<ResourceRevision, ResourceRevisionAccessor>(EntityManager.resourceRevisions,
						resourceRevisionAccessor, ResourceRevision.class, new ResourceRevisionsImporter(context)))
			.register(new Entity<DashboardSession, AbstractCRUDAccessor<DashboardSession>>("sessions",
					new AbstractCRUDAccessor<DashboardSession>(mongoClientSession,"sessions", DashboardSession.class),
					DashboardSession.class, new GenericDBImporter<DashboardSession, AbstractCRUDAccessor<DashboardSession>>(context) {
					}
					));
		context.getEntityManager().getEntityByName("sessions").setByPassObjectPredicate(true);

		createOrUpdateIndexes();

	}

	private void createOrUpdateIndexes() {
		long dataTTL = context.getConfiguration().getPropertyAsInteger("db.datattl", 0);
		context.getReportAccessor().createIndexesIfNeeded(dataTTL);
		context.getExecutionAccessor().createIndexesIfNeeded(dataTTL);
	}

	public void destroy() {
		// waits for executions to terminate
		scheduler.shutdown();

		serviceRegistrationCallback.stop();

		// call shutdown hooks
		pluginManager.getProxy().executionControllerDestroy(context);
		
		if(mongoClientSession !=null) {
			try {
				mongoClientSession.close();
			} catch (IOException e) {
				logger.error("Error while closing mongo client", e);
			}
		}		
	}
	
	private void recover() {
		ExecutionAccessor accessor = context.getExecutionAccessor();
		List<Execution> executions = accessor.getActiveTests();
		if(executions!=null && executions.size()>0) {
			logger.warn("Found " + executions.size() + " executions in an incosistent state. The system might not have been shutdown cleanly or crashed."
					+ "Starting recovery...");
			for(Execution e:executions) {
				logger.warn("Recovering test execution " + e.toString());
				logger.debug("Setting status to ENDED. TestExecutionID:"+ e.getId().toString()); 
				e.setStatus(ExecutionStatus.ENDED);
				e.setEndTime(System.currentTimeMillis());
				accessor.save(e);
			}
			logger.debug("Recovery ended.");
		}
	}
		
	public GlobalContext getContext() {
		return context;
	}

	public ExecutionScheduler getScheduler() {
		return scheduler;
	}
	
	public interface ServiceRegistrationCallback {
		
		public void registerService(Class<?> serviceClass);
		
		public void registerHandler(Handler handler);
		
		public void stop();
	}
	
}
