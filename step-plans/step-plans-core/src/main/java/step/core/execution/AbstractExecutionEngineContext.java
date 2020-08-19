package step.core.execution;

import ch.exense.commons.app.Configuration;
import step.core.AbstractContext;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.InMemoryExecutionAccessor;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.PlanAccessor;
import step.core.repositories.RepositoryObjectManager;
import step.engine.execution.ExecutionManager;
import step.engine.execution.ExecutionManagerImpl;
import step.expressions.ExpressionHandler;

public abstract class AbstractExecutionEngineContext extends AbstractContext {

	private Configuration configuration;
	private ExpressionHandler expressionHandler;
	private DynamicBeanResolver dynamicBeanResolver;
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
		configuration = new Configuration();
		expressionHandler = new ExpressionHandler();
		dynamicBeanResolver = new DynamicBeanResolver(new DynamicValueResolver(expressionHandler));
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
		configuration = parentContext.getConfiguration();
		expressionHandler = parentContext.getExpressionHandler();
		dynamicBeanResolver = parentContext.getDynamicBeanResolver();
		repositoryObjectManager = parentContext.getRepositoryObjectManager();
	}
	
	protected void useSourceAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
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

	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}
	
	public void setExpressionHandler(ExpressionHandler expressionHandler) {
		this.expressionHandler = expressionHandler;
	}
	
	public DynamicBeanResolver getDynamicBeanResolver() {
		return dynamicBeanResolver;
	}
	
	public void setDynamicBeanResolver(DynamicBeanResolver dynamicBeanResolver) {
		this.dynamicBeanResolver = dynamicBeanResolver;
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
