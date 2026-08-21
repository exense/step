package step.ide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.execution.ExecutionDiversion;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.ide.api.LocalFileSystemServices;
import step.ide.api.LocalIDEServices;
import step.resources.ResourceManagerImpl;

import java.io.File;

@Plugin
public class LocalIDEControllerPlugin extends AbstractControllerPlugin {
    private static final Logger logger = LoggerFactory.getLogger(LocalIDEControllerPlugin.class);

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        logger.debug("LocalIDEControllerPlugin serverStart");
        var state = LocalIDEState.get();

        state.setResourceManager((ResourceManagerImpl) context.getResourceManager());
        state.setFileResolver(context.getFileResolver());
        state.setConfiguration(context.getConfiguration());
        context.put(ExecutionDiversion.class, state);

        var services = context.getServiceRegistrationCallback();
        services.registerService(LocalIDEServices.class);
        services.registerService(LocalFileSystemServices.class);
    }

    @Override
    public void finalizeStart(GlobalContext context) throws Exception {
        logger.debug("LocalIDEControllerPlugin finalizeStart");
        LocalIDEState.get().onStartupFinished();
    }

    @Override
    public void postShutdownHook() {
        LocalIDEState.get().onShutdown();
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new IDEKeywordPropertiesPlugin(LocalIDEState.get().getConfiguration());
    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {
        LocalIDEState.get().useExistingAutomationPackageDirectory(new File("/Users/cyril/exense/step-backend/step/step-ap-ide/work").toPath());
    }
}
