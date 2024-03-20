package step.artefacts.handlers.functions;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
public class AutoscalerExecutionPlugin extends AbstractExecutionEnginePlugin {

    public static final String CONTEXT_OBJECT_KEY = "parallelism";
    private RootTokenNumberCalculationContext rootTokenNumberCalculationContext;

    @Override
    public void executionStart(ExecutionContext context) {
        super.executionStart(context);
        rootTokenNumberCalculationContext = new RootTokenNumberCalculationContext();
        context.getVariablesManager().putVariable(context.getReport(), CONTEXT_OBJECT_KEY, rootTokenNumberCalculationContext);
    }

    @Override
    public void afterReportNodeSkeletonCreation(ExecutionContext context, ReportNode node) {
        super.afterReportNodeSkeletonCreation(context, node);
        if(node.getArtefactID() == context.getPlan().getRoot().getId()) {
            // TODO remove
            System.out.println("Calculated following token requirements: " + rootTokenNumberCalculationContext.getRequiredTokensPerPool());
        }
    }

    public static TokenNumberCalculationContext getTokenNumberCalculationContext(ExecutionContext executionContext) {
        return (TokenNumberCalculationContext) executionContext.getVariablesManager().getVariable(CONTEXT_OBJECT_KEY);
    }

    public static void pushNewTokenNumberCalculationContext(ExecutionContext executionContext, TokenNumberCalculationContext newTokenNumberCalculationContext) {
        TokenNumberCalculationContext currentCalculationContext = getTokenNumberCalculationContext(executionContext);
        newTokenNumberCalculationContext.setParent(currentCalculationContext);
        executionContext.getVariablesManager().putVariable(executionContext.getCurrentReportNode(), CONTEXT_OBJECT_KEY, newTokenNumberCalculationContext);
    }
}
