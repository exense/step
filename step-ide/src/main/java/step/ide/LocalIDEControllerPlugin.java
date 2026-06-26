package step.ide;

import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.execution.ExecutionDiversion;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.ide.api.LocalFileSystemServices;
import step.ide.api.LocalIDEServices;
import step.resources.ResourceManagerImpl;

import java.io.File;

@Plugin
public class LocalIDEControllerPlugin extends AbstractControllerPlugin {
    @Override
    public void serverStart(GlobalContext context) throws Exception {
        System.out.println(this + " serverStart");
        LocalIDEState.get().setResourceManager((ResourceManagerImpl) context.getResourceManager());
        LocalIDEState.get().setFileResolver(context.getFileResolver());
        var services = context.getServiceRegistrationCallback();
        services.registerService(LocalIDEServices.class);
        services.registerService(LocalFileSystemServices.class);
        context.put(ExecutionDiversion.class, LocalIDEState.get());
    }
    /*
    @Override
    public void initializeData(GlobalContext context) throws Exception {
        LocalIDEState.get().useExistingAutomationPackageDirectory(new File("/Users/cyril/exense/step-backend/step/step-ap-ide/work"));
    }*/
}
