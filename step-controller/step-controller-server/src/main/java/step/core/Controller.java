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
import com.sun.xml.bind.v2.ContextFactory;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.core.access.User;
import step.core.access.UserAccessorImpl;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractUser;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessorImpl;
import step.core.collections.*;
import step.core.controller.SessionResponseBuilder;
import step.core.controller.settings.SettingScopeHandler;
import step.core.controller.settings.SettingScopeRegistry;
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
import step.engine.execution.ExecutionManagerImpl;
import step.expressions.ExpressionHandler;
import step.framework.server.ServerPluginManager;
import step.framework.server.ServiceRegistrationCallback;
import step.framework.server.Session;
import step.framework.server.access.AuthorizationManager;
import step.framework.server.access.NoAuthorizationManager;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.resources.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Controller {

	public static final Version VERSION = new Version(3,25,0);

	public static String USER_ACTIVITY_MAP_KEY = "userActivityMap";
	public static final String USER = "user";
	private Configuration configuration;
	
	private GlobalContext context;
	
	private ServiceRegistrationCallback serviceRegistrationCallback;
	
	private CollectionFactory collectionFactory;

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
		
		collectionFactory = CollectionFactoryConfigurationParser.parseConfiguration(configuration);
		context.setCollectionFactory(collectionFactory);

		context.setContorllerPluginManager(new ControllerPluginManager(context.require(ServerPluginManager.class)));

		NoAuthorizationManager noAccessManager = new NoAuthorizationManager();
		context.put(AuthorizationManager.class, noAccessManager);

		SessionResponseBuilder sessionResponseBuilder = new SessionResponseBuilder();
		context.put(SessionResponseBuilder.class, sessionResponseBuilder);
		sessionResponseBuilder.registerHook(s -> Map.of("username", s.getUser().getSessionUsername()));
		//AuthorizationManager might get overwritten by plugins, FE still need a default role in OS
		sessionResponseBuilder.registerHook(s -> Map.of("role", context.get(AuthorizationManager.class).getRoleInContext(s)));
		sessionResponseBuilder.registerHook(s -> Map.of("authenticated", s.isAuthenticated()));

		ResourceAccessor resourceAccessor = new ResourceAccessorImpl(collectionFactory.getCollection("resources", Resource.class));
		ResourceRevisionAccessor resourceRevisionAccessor = new ResourceRevisionAccessorImpl(
				collectionFactory.getCollection("resourceRevisions", ResourceRevision.class));
		String resourceRootDir = ResourceManagerControllerPlugin.getResourceDir(configuration);
		ResourceManager resourceManager = new ResourceManagerImpl(new File(resourceRootDir), resourceAccessor, resourceRevisionAccessor);
		context.setResourceManager(resourceManager);

		TableRegistry tableRegistry = new TableRegistry();
		context.put(TableRegistry.class, tableRegistry);		
		ExecutionAccessorImpl executionAccessor = new ExecutionAccessorImpl(
				collectionFactory.getCollection("executions", Execution.class));
		context.setExecutionAccessor(executionAccessor);		
		context.setExecutionManager(new ExecutionManagerImpl(executionAccessor));
		
		PlanAccessorImpl plans = new PlanAccessorImpl(collectionFactory.getCollection("plans", Plan.class));
		context.setPlanAccessor(plans);

		context.setReportNodeAccessor(
				new ReportNodeAccessorImpl(collectionFactory.getCollection("reports", ReportNode.class)));
		context.setScheduleAccessor(new ExecutionTaskAccessorImpl(
				collectionFactory.getCollection("tasks", ExecutiontTaskParameters.class)));

		Collection<User> userCollection = collectionFactory.getCollection("users", User.class);
		context.setUserAccessor(new UserAccessorImpl(userCollection));
		tableRegistry.register("users", new Table<>(userCollection, "user-read",false));

		SettingScopeRegistry settingScopeRegistry = new SettingScopeRegistry();
		settingScopeRegistry.register(USER, new SettingScopeHandler(USER) {
			@Override
			protected String getScopeValue(Session<?> session) {
				AbstractUser user = session.getUser();
				return (user != null) ? user.getSessionUsername() : null;
			}

			@Override
			public int getPriority() {
				return 1000;
			}

		});
		context.put(SettingScopeRegistry.class, settingScopeRegistry);
		
		//Im memory map to store last user activities
		Map<String, Long> userActivityMap = new ConcurrentHashMap<>();
		context.put(USER_ACTIVITY_MAP_KEY, userActivityMap);


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
				.register(new ResourceEntity(resourceAccessor, resourceManager, context.getFileResolver(), entityManager))
				.register(new Entity<>(EntityManager.resourceRevisions, resourceRevisionAccessor, ResourceRevision.class));
		
		entityManager.registerImportHook(new ResourceImporter(context.getResourceManager()));

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

	public void postShutdownHook() throws IOException {
		collectionFactory.close();
	}


}
