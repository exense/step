package step.plugins.remote;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class RemoteCollectionPlugin extends AbstractControllerPlugin {
    @Override
    public void serverStart(GlobalContext context) {
        context.getServiceRegistrationCallback().registerService(RemoteCollectionServices.class);
    }
}
