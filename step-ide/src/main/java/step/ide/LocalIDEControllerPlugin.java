package step.ide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackagePlugin;
import step.automation.packages.AutomationPackageReaderRegistry;
import step.automation.packages.LocalApResourceProvider;
import step.automation.packages.LocalAutomationPackageDirectoryProvider;
import step.core.GlobalContext;
import step.core.controller.ControllerSettingAccessor;
import step.core.execution.ExecutionDiversion;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.scheduler.ExecutionScheduler;
import step.ide.api.LocalFileSystemServices;
import step.ide.api.LocalIDEServices;
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
        model.setAutomationPackageReaderRegistry(context.require(AutomationPackageReaderRegistry.class));
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
    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {
        // The scheduling is switched off by LocalIDE: the setting is aligned so that the scheduler is reported as disabled
        context.require(ControllerSettingAccessor.class).updateOrCreateSetting(ExecutionScheduler.SETTING_SCHEDULER_ENABLED, Boolean.FALSE.toString());
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
