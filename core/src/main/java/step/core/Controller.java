package step.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.accessors.MongoDBAccessorHelper;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.ExecutionLifecycleManager;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessor;

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
		context.setExecutionAccessor(new ExecutionAccessor(mongoClient));
		context.setArtefactAccessor(new ArtefactAccessor(mongoClient));
		context.setReportAccessor(new ReportNodeAccessor(mongoClient));
		context.setScheduleAccessor(new ExecutionTaskAccessor(mongoClient));
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getArtefactAccessor()));
		context.setExecutionLifecycleManager(new ExecutionLifecycleManager(context));
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
	}
	
}
