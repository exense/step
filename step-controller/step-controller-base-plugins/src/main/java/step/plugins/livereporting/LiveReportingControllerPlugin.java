package step.plugins.livereporting;

import step.core.GlobalContext;
import step.core.controller.StepControllerPlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.livereporting.LiveReportingContexts;

@Plugin
public class LiveReportingControllerPlugin extends AbstractControllerPlugin {

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        String controllerUrl = StepControllerPlugin.getControllerUrl(context.getConfiguration(), true, true);
        LiveReportingContexts liveReportingContexts = new LiveReportingContexts(controllerUrl + "/rest/reporting");
        context.put(LiveReportingContexts.class, liveReportingContexts);

        context.getServiceRegistrationCallback().registerService(LiveReportingServices.class);
    }
}
