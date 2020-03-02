package step.core.execution;

import step.core.GlobalContext;
import step.core.accessors.collections.CollectionRegistry;
import step.core.execution.table.ExecutionCollection;
import step.core.execution.table.LeafReportNodeCollection;
import step.core.execution.table.ReportNodeCollection;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ExecutionTypePlugin.class, ScreenTemplatePlugin.class})
public class ExecutionPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		CollectionRegistry collectionRegistry = context.get(CollectionRegistry.class);
		collectionRegistry.register("executions", new ExecutionCollection(context));
		collectionRegistry.register("leafReports", new LeafReportNodeCollection(context));
		collectionRegistry.register("reports", new ReportNodeCollection(context));
	}
}
