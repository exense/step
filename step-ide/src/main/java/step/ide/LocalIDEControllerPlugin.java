package step.ide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.execution.ExecutionDiversion;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.ide.api.LocalFileSystemServices;
import step.ide.api.LocalIDEServices;
import step.resources.ResourceManagerImpl;

@Plugin
public class LocalIDEControllerPlugin extends AbstractControllerPlugin {
    private static final Logger logger = LoggerFactory.getLogger(LocalIDEControllerPlugin.class);

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        logger.debug("LocalIDEControllerPlugin serverStart");
        var state = LocalIDEState.get();

        state.setResourceManager((ResourceManagerImpl) context.getResourceManager());
        state.setFileResolver(context.getFileResolver());
        context.put(ExecutionDiversion.class, state);

        var services = context.getServiceRegistrationCallback();
        services.registerService(LocalIDEServices.class);
        services.registerService(LocalFileSystemServices.class);
    }

    @Override
    public void postShutdownHook() {
        LocalIDEState.get().onShutdown();
    }
}
