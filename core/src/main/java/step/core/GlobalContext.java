package step.core;

import step.core.Controller.ServiceRegistrationCallback;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.ExecutionLifecycleManager;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionTaskAccessor;

import com.mongodb.MongoClient;

public class GlobalContext extends AbstractContext {
	
	private PluginManager pluginManager;
	
	private RepositoryObjectManager repositoryObjectManager;
	
	private MongoClient mongoClient;
	
	private ExecutionAccessor executionAccessor;
	
	private ArtefactAccessor artefactAccessor;
	
	private ReportNodeAccessor reportAccessor;
	
	private ExecutionTaskAccessor scheduleAccessor;
	
	private ExecutionLifecycleManager executionLifecycleManager;
	
	private ServiceRegistrationCallback serviceRegistrationCallback;

	public GlobalContext() {
		super();
	}

	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public void setMongoClient(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}

	public ExecutionAccessor getExecutionAccessor() {
		return executionAccessor;
	}

	public void setExecutionAccessor(ExecutionAccessor executionAccessor) {
		this.executionAccessor = executionAccessor;
	}

	public ArtefactAccessor getArtefactAccessor() {
		return artefactAccessor;
	}

	public void setArtefactAccessor(ArtefactAccessor artefactAccessor) {
		this.artefactAccessor = artefactAccessor;
	}

	public ReportNodeAccessor getReportAccessor() {
		return reportAccessor;
	}

	public void setReportAccessor(ReportNodeAccessor reportAccessor) {
		this.reportAccessor = reportAccessor;
	}

	public ExecutionTaskAccessor getScheduleAccessor() {
		return scheduleAccessor;
	}

	public void setScheduleAccessor(ExecutionTaskAccessor scheduleAccessor) {
		this.scheduleAccessor = scheduleAccessor;
	}

	public PluginManager getPluginManager() {
		return pluginManager;
	}

	public void setPluginManager(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}

	public RepositoryObjectManager getRepositoryObjectManager() {
		return repositoryObjectManager;
	}

	public void setRepositoryObjectManager(
			RepositoryObjectManager repositoryObjectManager) {
		this.repositoryObjectManager = repositoryObjectManager;
	}

	public ExecutionLifecycleManager getExecutionLifecycleManager() {
		return executionLifecycleManager;
	}

	public void setExecutionLifecycleManager(
			ExecutionLifecycleManager executionLifecycleManager) {
		this.executionLifecycleManager = executionLifecycleManager;
	}
	
	public ServiceRegistrationCallback getServiceRegistrationCallback() {
		return serviceRegistrationCallback;
	}

	public void setServiceRegistrationCallback(
			ServiceRegistrationCallback serviceRegistrationCallback) {
		this.serviceRegistrationCallback = serviceRegistrationCallback;
	}
	
}
