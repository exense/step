package step.core.execution;

import ch.exense.commons.app.Configuration;
import org.bson.types.ObjectId;
import step.attachments.FileResolver;
import step.core.AbstractContext;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plugins.ExecutionCallbacks;
import step.core.repositories.RepositoryObjectManager;
import step.core.resolvers.Resolver;
import step.core.variables.VariablesManager;
import step.engine.execution.ExecutionManager;
import step.expressions.ExpressionHandler;
import step.resources.ResourceAccessor;
import step.resources.ResourceManager;

import java.io.IOException;
import java.util.function.Function;

public class ExecutionContextWrapper extends ExecutionContext {

    private ExecutionContext wrappedContext;

    public ExecutionContextWrapper(ExecutionContext wrappedContext) {
        super((new ObjectId()).toString(), null);
        this.wrappedContext = wrappedContext;
    }
    
    @Override
    public Object get(Object key) {
        return wrappedContext.get(key);
    }

    @Override
    public <T> T get(Class<T> class_) {
        return wrappedContext.get(class_);
    }

    @Override
    public <T> T require(Class<T> class_) {
        return wrappedContext.require(class_);
    }

    @Override
    public <T> T computeIfAbsent(Class<T> class_, Function<Class<T>, T> mappingFunction) {
        return wrappedContext.computeIfAbsent(class_, mappingFunction);
    }

    @Override
    public Object put(String key, Object value) {
        return wrappedContext.put(key, value);
    }

    @Override
    public <T> T put(Class<T> class_, T value) {
        return wrappedContext.put(class_, value);
    }

    @Override
    public <T> T inheritFromParentOrComputeIfAbsent(AbstractContext parentContext, Class<T> class_, Function<Class<T>, T> mappingFunction) {
        return wrappedContext.inheritFromParentOrComputeIfAbsent(parentContext, class_, mappingFunction);
    }

    @Override
    public void close() throws IOException {
        //wrappedContext.close();
    }

    @Override
    public ExpressionHandler getExpressionHandler() {
        return wrappedContext.getExpressionHandler();
    }

    @Override
    public void setExpressionHandler(ExpressionHandler expressionHandler) {
        wrappedContext.setExpressionHandler(expressionHandler);
    }

    @Override
    public DynamicBeanResolver getDynamicBeanResolver() {
        return wrappedContext.getDynamicBeanResolver();
    }

    @Override
    public void setDynamicBeanResolver(DynamicBeanResolver dynamicBeanResolver) {
        wrappedContext.setDynamicBeanResolver(dynamicBeanResolver);
    }

    @Override
    public ResourceAccessor getResourceAccessor() {
        return wrappedContext.getResourceAccessor();
    }

    @Override
    public void setResourceAccessor(ResourceAccessor resourceAccessor) {
        wrappedContext.setResourceAccessor(resourceAccessor);
    }

    @Override
    public ResourceManager getResourceManager() {
        return wrappedContext.getResourceManager();
    }

    @Override
    public void setResourceManager(ResourceManager resourceManager) {
        wrappedContext.setResourceManager(resourceManager);
    }

    @Override
    public FileResolver getFileResolver() {
        return wrappedContext.getFileResolver();
    }

    @Override
    public void setFileResolver(FileResolver fileResolver) {
        wrappedContext.setFileResolver(fileResolver);
    }

    @Override
    public void setDefaultAttributes() {
       // wrappedContext.setDefaultAttributes();
    }

    @Override
    public void useAllAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
        wrappedContext.useAllAttributesFromParentContext(parentContext);
    }

    @Override
    public void useStandardAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
        wrappedContext.useStandardAttributesFromParentContext(parentContext);
    }

    @Override
    public void useSourceAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
        wrappedContext.useSourceAttributesFromParentContext(parentContext);
    }

    @Override
    public void useReportingAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
        wrappedContext.useReportingAttributesFromParentContext(parentContext);
    }

    @Override
    public Configuration getConfiguration() {
        return wrappedContext.getConfiguration();
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        wrappedContext.setConfiguration(configuration);
    }

    @Override
    public ArtefactHandlerRegistry getArtefactHandlerRegistry() {
        return wrappedContext.getArtefactHandlerRegistry();
    }

    @Override
    public void setArtefactHandlerRegistry(ArtefactHandlerRegistry artefactHandlerRegistry) {
        wrappedContext.setArtefactHandlerRegistry(artefactHandlerRegistry);
    }

    @Override
    public PlanAccessor getPlanAccessor() {
        return wrappedContext.getPlanAccessor();
    }

    @Override
    public void setPlanAccessor(PlanAccessor planAccessor) {
        wrappedContext.setPlanAccessor(planAccessor);
    }

    @Override
    public ReportNodeAccessor getReportAccessor() {
        return wrappedContext.getReportAccessor();
    }

    @Override
    public ReportNodeAccessor getReportNodeAccessor() {
        return wrappedContext.getReportNodeAccessor();
    }

    @Override
    public void setReportNodeAccessor(ReportNodeAccessor reportNodeAccessor) {
        wrappedContext.setReportNodeAccessor(reportNodeAccessor);
    }

    @Override
    public ExecutionAccessor getExecutionAccessor() {
        return wrappedContext.getExecutionAccessor();
    }

    @Override
    public void setExecutionAccessor(ExecutionAccessor executionAccessor) {
        wrappedContext.setExecutionAccessor(executionAccessor);
    }

    @Override
    public ExecutionManager getExecutionManager() {
        return wrappedContext.getExecutionManager();
    }

    @Override
    public void setExecutionManager(ExecutionManager executionManager) {
        wrappedContext.setExecutionManager(executionManager);
    }

    @Override
    public RepositoryObjectManager getRepositoryObjectManager() {
        return wrappedContext.getRepositoryObjectManager();
    }

    @Override
    public void setRepositoryObjectManager(RepositoryObjectManager repositoryObjectManager) {
        wrappedContext.setRepositoryObjectManager(repositoryObjectManager);
    }

    @Override
    public ArtefactHandlerManager getArtefactHandlerManager() {
        return wrappedContext.getArtefactHandlerManager();
    }

    @Override
    public String getExecutionType() {
        return wrappedContext.getExecutionType();
    }

    @Override
    public void setExecutionType(String executionType) {
        wrappedContext.setExecutionType(executionType);
    }

    @Override
    public Plan getPlan() {
        return wrappedContext.getPlan();
    }

    @Override
    public void setPlan(Plan plan) {
        wrappedContext.setPlan(plan);
    }

    @Override
    public ReportNode getReport() {
        return wrappedContext.getReport();
    }

    @Override
    public ReportNodeCache getReportNodeCache() {
        if (wrappedContext != null) {
            return wrappedContext.getReportNodeCache();
        } else {
           return  super.getReportNodeCache();
        }
    }

    @Override
    public ReportNode getCurrentReportNode() {
        return wrappedContext.getCurrentReportNode();
    }

    @Override
    public void setCurrentReportNode(ReportNode node) {
        if (wrappedContext != null) {
            wrappedContext.setCurrentReportNode(node);
        } else {
            super.setCurrentReportNode(node);
        }
    }

    @Override
    public void associateThread() {
        wrappedContext.associateThread();
    }

    @Override
    public void associateThread(long parentThreadId, ReportNode currentReportNode) {
        wrappedContext.associateThread(parentThreadId, currentReportNode);
    }

    @Override
    public String getExecutionId() {
        return wrappedContext.getExecutionId();
    }

    @Override
    public ExecutionStatus getStatus() {
        return wrappedContext.getStatus();
    }

    @Override
    public boolean isInterrupted() {
        return wrappedContext.isInterrupted();
    }

    @Override
    public boolean isSimulation() {
        return wrappedContext.isSimulation();
    }

    @Override
    public void updateStatus(ExecutionStatus status) {
        wrappedContext.updateStatus(status);
    }

    @Override
    public VariablesManager getVariablesManager() {
        return wrappedContext.getVariablesManager();
    }

    @Override
    public String toString() {
        return wrappedContext.toString();
    }

    @Override
    public ExecutionParameters getExecutionParameters() {
        return wrappedContext.getExecutionParameters();
    }

    @Override
    public EventManager getEventManager() {
        return wrappedContext.getEventManager();
    }

    @Override
    public ExecutionCallbacks getExecutionCallbacks() {
        return wrappedContext.getExecutionCallbacks();
    }

    @Override
    public void setExecutionCallbacks(ExecutionCallbacks executionCallbacks) {
        wrappedContext.setExecutionCallbacks(executionCallbacks);
    }

    @Override
    public ObjectEnricher getObjectEnricher() {
        return wrappedContext.getObjectEnricher();
    }

    @Override
    public void setObjectEnricher(ObjectEnricher objectEnricher) {
        wrappedContext.setObjectEnricher(objectEnricher);
    }

    @Override
    public ObjectPredicate getObjectPredicate() {
        return wrappedContext.getObjectPredicate();
    }

    @Override
    public void setObjectPredicate(ObjectPredicate objectPredicate) {
        wrappedContext.setObjectPredicate(objectPredicate);
    }

    @Override
    public Resolver getResolver() {
        return wrappedContext.getResolver();
    }
    
    
}
