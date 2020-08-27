package step.core.execution;

import ch.exense.commons.app.Configuration;
import step.core.AbstractStepContext;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.InMemoryExecutionAccessor;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.PlanAccessor;
import step.core.repositories.RepositoryObjectManager;
import step.engine.execution.ExecutionManager;
import step.engine.execution.ExecutionManagerImpl;

public abstract class AbstractExecutionEngineContext extends AbstractStepContext {

	private Configuration configuration;

	private ArtefactHandlerRegistry artefactHandlerRegistry;

	private PlanAccessor planAccessor;
	private ReportNodeAccessor reportNodeAccessor;
	private ExecutionAccessor executionAccessor;

	private ExecutionManager executionManager;
	private RepositoryObjectManager repositoryObjectManager;
	
	public AbstractExecutionEngineContext() {
		super();
		setDefaultAttributes();
	}

	protected void setDefaultAttributes() {
		super.setDefaultAttributes();
		configuration = new Configuration();

		artefactHandlerRegistry = new ArtefactHandlerRegistry();

		planAccessor = new InMemoryPlanAccessor();
		reportNodeAccessor = new InMemoryReportNodeAccessor();
		executionAccessor = new InMemoryExecutionAccessor();

		executionManager = new ExecutionManagerImpl(executionAccessor);
		repositoryObjectManager = new RepositoryObjectManager();
	}
	
	protected void useAllAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
		useStandardAttributesFromParentContext(parentContext);
		useSourceAttributesFromParentContext(parentContext);
		useReportingAttributesFromParentContext(parentContext);
	}
	
	protected void useStandardAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
		super.useStandardAttributesFromParentContext(parentContext);
		configuration = parentContext.getConfiguration();
		repositoryObjectManager = parentContext.getRepositoryObjectManager();
		artefactHandlerRegistry = parentContext.getArtefactHandlerRegistry();
	}
	
	protected void useSourceAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
		super.useSourceAttributesFromParentContext(parentContext);
		planAccessor = parentContext.getPlanAccessor();
	}
	
	protected void useReportingAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
		reportNodeAccessor = parentContext.getReportNodeAccessor();
		executionAccessor = parentContext.getExecutionAccessor();
		executionManager = parentContext.getExecutionManager();
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public ArtefactHandlerRegistry getArtefactHandlerRegistry() {
		return artefactHandlerRegistry;
	}

	public void setArtefactHandlerRegistry(ArtefactHandlerRegistry artefactHandlerRegistry) {
		this.artefactHandlerRegistry = artefactHandlerRegistry;
	}

	public PlanAccessor getPlanAccessor() {
		return planAccessor;
	}
	
	public void setPlanAccessor(PlanAccessor planAccessor) {
		this.planAccessor = planAccessor;
	}
	
	public ReportNodeAccessor getReportAccessor() {
		return getReportNodeAccessor();
	}
	
	public ReportNodeAccessor getReportNodeAccessor() {
		return reportNodeAccessor;
	}
	
	public void setReportNodeAccessor(ReportNodeAccessor reportNodeAccessor) {
		this.reportNodeAccessor = reportNodeAccessor;
	}

	public ExecutionAccessor getExecutionAccessor() {
		return executionAccessor;
	}

	public void setExecutionAccessor(ExecutionAccessor executionAccessor) {
		this.executionAccessor = executionAccessor;
	}

	public ExecutionManager getExecutionManager() {
		return executionManager;
	}

	public void setExecutionManager(ExecutionManager executionManager) {
		this.executionManager = executionManager;
	}

	public RepositoryObjectManager getRepositoryObjectManager() {
		return repositoryObjectManager;
	}

	public void setRepositoryObjectManager(RepositoryObjectManager repositoryObjectManager) {
		this.repositoryObjectManager = repositoryObjectManager;
	}
}
