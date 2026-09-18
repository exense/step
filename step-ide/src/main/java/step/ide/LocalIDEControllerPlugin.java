package step.ide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackagePlugin;
import step.automation.packages.LocalApResourceProvider;
import step.automation.packages.LocalAutomationPackageDirectoryProvider;
import step.core.GlobalContext;
import step.core.execution.ExecutionDiversion;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.ide.api.LocalFileSystemServices;
import step.ide.api.LocalIDEServices;
import step.resources.ResourceManagerImpl;

@Plugin(dependencies = AutomationPackagePlugin.class)
public class LocalIDEControllerPlugin extends AbstractControllerPlugin {
    private static final Logger logger = LoggerFactory.getLogger(LocalIDEControllerPlugin.class);

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        logger.debug("LocalIDEControllerPlugin serverStart");
        var state = LocalIDEState.get();

        state.setResourceManager((ResourceManagerImpl) context.getResourceManager());
        state.setFileResolver(context.getFileResolver());
        context.put(ExecutionDiversion.class, state);
        // Lets the automation package services browse the package open in the editor under the 'local'
        // id, so that the IDE and a Step server expose the very same ap-resource services.
        context.put(LocalAutomationPackageDirectoryProvider.class, state::getCurrentAutomationPackageDirectory);

        var services = context.getServiceRegistrationCallback();
        services.registerService(LocalIDEServices.class);
        services.registerService(LocalFileSystemServices.class);

        context.setApResourceProvider(new LocalApResourceProvider(
            () -> LocalIDEState.get().getCurrentAutomationPackageDirectory(),
            context.getApResourceProvider()));
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
}
