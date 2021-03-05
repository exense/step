package step.plugins.measurements;

import step.controller.grid.GridPlugin;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.grid.Grid;
import step.grid.GridReportBuilder;
import step.grid.TokenWrapperState;
import step.grid.reports.TokenGroupCapacity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Plugin(dependencies = GridPlugin.class)
public class MeasurementControllerPlugin extends AbstractControllerPlugin {

	GaugeCollectorRegistry gaugeCollectorRegistry;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);

		gaugeCollectorRegistry = GaugeCollectorRegistry.getInstance();
		//Register grid gauge metrics
		gaugeCollectorRegistry.registerCollector("step_grid_tokens",new GaugeCollector() {
			final String[] labels = {"type","url","metric"};
			final List<String> groupBy = Arrays.asList(new String[]{"$agenttype","url"});
			final GridReportBuilder gridReportBuilder = new GridReportBuilder((Grid) context.get(Grid.class));
			@Override
			public String getName() {
				return "step_grid_tokens";
			}
			@Override
			public String getDescription() {
				return "step grid token usage and capacity";
			}
			@Override
			public String[] getLabels() {
				return labels;
			}
			@Override
			public List<GaugeMetric> collect() {
				List<GaugeMetric> results = new ArrayList<>();
				List<TokenGroupCapacity> usageByIdentity = gridReportBuilder.getUsageByIdentity(groupBy);
				for (TokenGroupCapacity tokenGroupCapacity : usageByIdentity) {
					Integer free = tokenGroupCapacity.getCountByState().get(TokenWrapperState.FREE);
					free = (free == null)  ? 0 : free;
					int capacity = tokenGroupCapacity.getCapacity();
					results.add(new GaugeMetric(Double.valueOf(free),tokenGroupCapacity.getKey().get("$agenttype"),
							tokenGroupCapacity.getKey().get("url"),"free"));
					results.add(new GaugeMetric(Double.valueOf(capacity),tokenGroupCapacity.getKey().get("$agenttype"),
							tokenGroupCapacity.getKey().get("url"),"capacity"));
				}
				return results;
			}
		});

		//Start the gauge scheduler
		gaugeCollectorRegistry.start();
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {

	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new MeasurementPlugin();
	}
}
