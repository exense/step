package step.ide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackagePlugin;
import step.automation.packages.LocalApResourceProvider;
import step.automation.packages.LocalAutomationPackageDirectoryProvider;
import step.core.GlobalContext;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.execution.ExecutionDiversion;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.ide.api.LocalFileSystemServices;
import step.ide.api.LocalIDEServices;
import step.ide.api.StepConnectionInfo;
import step.resources.ResourceManagerImpl;

@Plugin(dependencies = AutomationPackagePlugin.class)
public class LocalIDEControllerPlugin extends AbstractControllerPlugin {
    private static final Logger logger = LoggerFactory.getLogger(LocalIDEControllerPlugin.class);

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        logger.debug("LocalIDEControllerPlugin serverStart");
        var model = LocalIDEModel.get();

        model.setResourceManager((ResourceManagerImpl) context.getResourceManager());
        model.setFileResolver(context.getFileResolver());
        context.put(ExecutionDiversion.class, model);
        // Lets the automation package services browse the package open in the editor under the 'local'
        // id, so that the IDE and a Step server expose the very same ap-resource services.
        context.put(LocalAutomationPackageDirectoryProvider.class, model::getCurrentAutomationPackageDirectory);

        var services = context.getServiceRegistrationCallback();
        services.registerService(LocalIDEServices.class);
        services.registerService(LocalFileSystemServices.class);

        context.setApResourceProvider(new LocalApResourceProvider(
            () -> LocalIDEModel.get().getCurrentAutomationPackageDirectory(),
            context.getApResourceProvider()));

        // This makes the CLI configuration (url, token etc.) available to the frontend.
        context.require(WebApplicationConfigurationManager.class)
            .registerHook(session -> StepConnectionInfo.toConfigurationMap(model.getCliConnection()));
    }

    @Override
    public void finalizeStart(GlobalContext context) throws Exception {
        logger.debug("LocalIDEControllerPlugin finalizeStart");
        LocalIDEModel.get().onStartupFinished();
    }

    @Override
    public void postShutdownHook() {
        LocalIDEModel.get().onShutdown();
    }
}
