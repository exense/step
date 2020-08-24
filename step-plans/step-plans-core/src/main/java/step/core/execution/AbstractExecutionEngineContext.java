package step.core.execution;

import java.io.File;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.AbstractContext;
import step.core.artefacts.ArtefactRegistry;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
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
import step.resources.InMemoryResourceAccessor;
import step.resources.InMemoryResourceRevisionAccessor;
import step.resources.ResourceAccessor;
import step.resources.ResourceManager;
import step.resources.ResourceManagerImpl;

public abstract class AbstractExecutionEngineContext extends AbstractContext {

	private Configuration configuration;
	private ExpressionHandler expressionHandler;
	private DynamicBeanResolver dynamicBeanResolver;

	private ArtefactRegistry artefactRegistry;
	private ArtefactHandlerRegistry artefactHandlerRegistry;
	
	private ResourceAccessor resourceAccessor;
	private PlanAccessor planAccessor;
	private ReportNodeAccessor reportNodeAccessor;
	private ExecutionAccessor executionAccessor;

	private ResourceManager resourceManager;
	private ExecutionManager executionManager;
	private RepositoryObjectManager repositoryObjectManager;

	private FileResolver fileResolver;
	
	public AbstractExecutionEngineContext() {
		super();
		setDefaultAttributes();
	}

	protected void setDefaultAttributes() {
		configuration = new Configuration();
		expressionHandler = new ExpressionHandler();
		dynamicBeanResolver = new DynamicBeanResolver(new DynamicValueResolver(expressionHandler));
		
		artefactRegistry = new ArtefactRegistry();
		artefactHandlerRegistry = new ArtefactHandlerRegistry();
		
		resourceAccessor = new InMemoryResourceAccessor();
		planAccessor = new InMemoryPlanAccessor();
		reportNodeAccessor = new InMemoryReportNodeAccessor();
		executionAccessor = new InMemoryExecutionAccessor();

		resourceManager = new ResourceManagerImpl(new File("resources"), resourceAccessor, new InMemoryResourceRevisionAccessor());
		executionManager = new ExecutionManagerImpl(executionAccessor);
		repositoryObjectManager = new RepositoryObjectManager();

		fileResolver = new FileResolver(resourceManager);
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
		artefactRegistry = parentContext.getArtefactRegistry();
		artefactHandlerRegistry = parentContext.getArtefactHandlerRegistry();
	}
	
	protected void useSourceAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
		planAccessor = parentContext.getPlanAccessor();
		resourceAccessor = parentContext.getResourceAccessor();
		resourceManager = parentContext.getResourceManager();
		fileResolver = parentContext.getFileResolver();
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

	public FileResolver getFileResolver() {
		return fileResolver;
	}

	public void setFileResolver(FileResolver fileResolver) {
		this.fileResolver = fileResolver;
	}

	public ResourceAccessor getResourceAccessor() {
		return resourceAccessor;
	}

	public void setResourceAccessor(ResourceAccessor resourceAccessor) {
		this.resourceAccessor = resourceAccessor;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public void setResourceManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
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
	
	public ArtefactRegistry getArtefactRegistry() {
		return artefactRegistry;
	}

	public void setArtefactRegistry(ArtefactRegistry artefactRegistry) {
		this.artefactRegistry = artefactRegistry;
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
