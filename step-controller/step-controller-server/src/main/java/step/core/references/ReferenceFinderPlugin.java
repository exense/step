package step.core.references;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ReferenceFinderPlugin extends AbstractControllerPlugin {
    @Override
    public void serverStart(GlobalContext context) throws Exception {
        context.getServiceRegistrationCallback().registerService(ReferenceFinderServices.class);
    }
}
