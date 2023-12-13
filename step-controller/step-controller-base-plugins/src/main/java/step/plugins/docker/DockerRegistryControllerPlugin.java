package step.plugins.docker;

import step.core.GlobalContext;
import step.core.docker.DockerRegistryConfiguration;
import step.core.docker.DockerRegistryConfigurationAccessor;
import step.core.entities.Entity;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.ExecutionEnginePlugin;

@Plugin
public class DockerRegistryControllerPlugin extends AbstractControllerPlugin {
    public static final String ENTITY_DOCKER_REGISTRIES = "dockerRegistries";
    protected DockerRegistryConfigurationAccessor dockerRegistryConfigurationAccessor;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        dockerRegistryConfigurationAccessor = new DockerRegistryConfigurationAccessor(context
                .getCollectionFactory().getCollection(ENTITY_DOCKER_REGISTRIES, DockerRegistryConfiguration.class));
        context.put(DockerRegistryConfigurationAccessor.class, dockerRegistryConfigurationAccessor);
        context.getEntityManager().register(new Entity<>(
                ENTITY_DOCKER_REGISTRIES,
                dockerRegistryConfigurationAccessor,
                DockerRegistryConfiguration.class
        ));
        context.getServiceRegistrationCallback().registerService(DockerRegistryServices.class);
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new AbstractExecutionEnginePlugin() {
            @Override
            public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
                super.initializeExecutionContext(executionEngineContext, executionContext);
                executionContext.put(DockerRegistryConfigurationAccessor.class, dockerRegistryConfigurationAccessor);
            }
        };
    }
}
