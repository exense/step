package step.plugins.docker;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class DockerRegistryControllerPlugin extends AbstractControllerPlugin {
    protected DockerRegistryConfigurationAccessor dockerRegistryConfigurationAccessor;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        dockerRegistryConfigurationAccessor = new DockerRegistryConfigurationAccessor(context
                .getCollectionFactory().getCollection("dockerRegistries", DockerRegistryConfiguration.class));
        context.put(DockerRegistryConfigurationAccessor.class, dockerRegistryConfigurationAccessor);
        context.getServiceRegistrationCallback().registerService(DockerRegistryServices.class);
    }
}
