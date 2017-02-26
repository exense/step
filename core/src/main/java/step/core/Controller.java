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

import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.conf.Configuration;
import step.core.access.UserAccessor;
import step.core.accessors.MongoDBAccessorHelper;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.ArtefactManager;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.ExecutionLifecycleManager;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessor;
import step.expressions.ExpressionHandler;

import com.mongodb.MongoClient;

public class Controller {
	
	private static final Logger logger = LoggerFactory.getLogger(Controller.class);
	
	private GlobalContext context;
		
	private PluginManager pluginManager;
	
	private ExecutionScheduler scheduler;
	
	public void init(ServiceRegistrationCallback serviceRegistrationCallback) throws Exception {			
		pluginManager = new PluginManager();
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
		
		MongoClient mongoClient = MongoDBAccessorHelper.getClient();
		context.setMongoClient(mongoClient);
		context.setMongoDatabase(MongoDBAccessorHelper.getInstance().getMongoDatabase(mongoClient));
		context.setExecutionAccessor(new ExecutionAccessor(mongoClient));
		context.setArtefactAccessor(new ArtefactAccessor(mongoClient));
		context.setArtefactManager(new ArtefactManager(context.getArtefactAccessor()));
		context.setReportAccessor(new ReportNodeAccessor(mongoClient));
		context.setScheduleAccessor(new ExecutionTaskAccessor(mongoClient));
		context.setUserAccessor(new UserAccessor(mongoClient));
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getArtefactAccessor()));
		context.setExecutionLifecycleManager(new ExecutionLifecycleManager(context));
		context.setExpressionHandler(new ExpressionHandler());
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		createOrUpdateIndexes();
	}

	private void createOrUpdateIndexes() {
		long dataTTL = Configuration.getInstance().getPropertyAsInteger("db.datattl", 0);
		context.getReportAccessor().createIndexesIfNeeded(dataTTL);
		context.getExecutionAccessor().createIndexesIfNeeded(dataTTL);
	}

	public void destroy() {
		//TODO wait for tasks to terminate
		scheduler.shutdown();

		pluginManager.getProxy().executionControllerDestroy(context);
		
		destroyContext();
	}
	
	private void destroyContext() {
		if(context.getMongoClient()!=null) {
			context.getMongoClient().close();
		}
		if(context.getRepositoryObjectManager()!=null) {
			context.getRepositoryObjectManager().close();
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
	}
	
}
