package step.ide;

import step.core.GlobalContext;
import step.core.execution.ExecutionDiversion;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.ide.api.LocalFileSystemServices;
import step.ide.api.LocalIDEServices;
import step.resources.ResourceManagerImpl;

@Plugin
public class LocalIDEControllerPlugin extends AbstractControllerPlugin {
    @Override
    public void serverStart(GlobalContext context) throws Exception {
        System.out.println(this + " serverStart");
        LocalIDEState.get().setResourceManager((ResourceManagerImpl) context.getResourceManager());
        var services = context.getServiceRegistrationCallback();
        services.registerService(LocalIDEServices.class);
        services.registerService(LocalFileSystemServices.class);
        context.put(ExecutionDiversion.class, LocalIDEState.get());
    }
}
