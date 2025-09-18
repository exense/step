package step.plugins.reporting;

import step.core.GlobalContext;
import step.core.controller.StepControllerPlugin;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.plugins.streaming.StreamingResourceServices;
import step.reporting.fixme.LiveMeasureContexts;

@Plugin // do we need any dependencies?
public class ReportingControllerPlugin extends AbstractControllerPlugin {

    private ReportingManager manager;



    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        String controllerUrl = StepControllerPlugin.getControllerUrl(context.getConfiguration(), true, true);
        manager = new ReportingManager(controllerUrl);
        context.put(ReportingManager.class, manager);

        context.getServiceRegistrationCallback().registerService(ReportingServices.class);
    }

    @Override
    public void serverStop(GlobalContext context) {
        super.serverStop(context);
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new AbstractExecutionEnginePlugin() {
            @Override
            public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
                executionContext.put(LiveMeasureContexts.class, manager.getLiveMeasureContexts());
            }
        };
    }
}
