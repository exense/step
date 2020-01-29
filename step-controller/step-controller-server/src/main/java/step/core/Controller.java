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
package step.core;

import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.core.access.UserAccessorImpl;
import step.core.accessors.CollectionRegistry;
import step.core.accessors.MongoClientSession;
import step.core.accessors.PlanAccessorImpl;
import step.core.artefacts.reports.ReportNodeAccessorImpl;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.EventManager;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionAccessorImpl;
import step.core.execution.model.ExecutionStatus;
import step.core.plugins.ControllerPluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessorImpl;
import step.expressions.ExpressionHandler;

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
		pluginManager = new ControllerPluginManager(configuration);
		pluginManager.initialize();
		
		initContext();
		context.setServiceRegistrationCallback(serviceRegistrationCallback);
		
		recover();
		
		pluginManager.getProxy().executionControllerStart(context);

		scheduler = new ExecutionScheduler(context);
		scheduler.start();
	}
	
	private void initContext() {
		context = new GlobalContext();
		context.setPluginManager(pluginManager);
		
		mongoClientSession = new MongoClientSession(configuration);
		
		context.setConfiguration(configuration);
		context.setMongoClientSession(mongoClientSession);
		context.put(CollectionRegistry.class, new CollectionRegistry());
		context.setExecutionAccessor(new ExecutionAccessorImpl(mongoClientSession));
		context.setPlanAccessor(new PlanAccessorImpl(mongoClientSession));
		context.setReportAccessor(new ReportNodeAccessorImpl(mongoClientSession));
		context.setScheduleAccessor(new ExecutionTaskAccessorImpl(mongoClientSession));
		context.setUserAccessor(new UserAccessorImpl(mongoClientSession));
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getPlanAccessor()));
		context.setExpressionHandler(new ExpressionHandler(configuration.getProperty("tec.expressions.scriptbaseclass"), 
				configuration.getPropertyAsInteger("tec.expressions.warningthreshold"),
				configuration.getPropertyAsInteger("tec.expressions.pool.maxtotal",1000),
				configuration.getPropertyAsInteger("tec.expressions.pool.maxidle",-1)));
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		context.setEventManager(new EventManager());
		
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
