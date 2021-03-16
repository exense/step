package step.plugins.measurements;

import io.prometheus.client.Collector;
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

import static step.plugins.measurements.MeasurementPlugin.*;

@Plugin(dependencies = GridPlugin.class)
public class MeasurementControllerPlugin extends AbstractControllerPlugin {

	public static String GridGaugeName = "step_grid_tokens";
	public static String ThreadgroupGaugeName = "step_threadgroup";

	GaugeCollectorRegistry gaugeCollectorRegistry;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);

		gaugeCollectorRegistry = GaugeCollectorRegistry.getInstance();
		//Register grid gauge metrics
		final String[] labels = {"agent_type","url","name"};
		gaugeCollectorRegistry.registerCollector(GridGaugeName,new GaugeCollector(GridGaugeName,
				"step grid token usage and capacity", labels) {
			final List<String> groupBy = Arrays.asList(new String[]{"$agenttype","url"});
			final GridReportBuilder gridReportBuilder = new GridReportBuilder((Grid) context.get(Grid.class));
			@Override
			public List<Collector.MetricFamilySamples> collect() {
				List<TokenGroupCapacity> usageByIdentity = gridReportBuilder.getUsageByIdentity(groupBy);
				for (TokenGroupCapacity tokenGroupCapacity : usageByIdentity) {
					Integer free = tokenGroupCapacity.getCountByState().get(TokenWrapperState.FREE);
					free = (free == null)  ? 0 : free;
					int capacity = tokenGroupCapacity.getCapacity();
					getGauge().labels(tokenGroupCapacity.getKey().get("$agenttype"),
							tokenGroupCapacity.getKey().get("url"),"free").set(Double.valueOf(free));
					getGauge().labels(tokenGroupCapacity.getKey().get("$agenttype"),
							tokenGroupCapacity.getKey().get("url"),"capacity").set(Double.valueOf(capacity));
				}
				return getGauge().collect();
			}
		});

		gaugeCollectorRegistry = GaugeCollectorRegistry.getInstance();
		final String[] labelsThreadGroup = {ATTRIBUTE_EXECUTION_ID,NAME,PLAN_ID,TASK_ID};
		//Register thread group gauge metrics
		gaugeCollectorRegistry.registerCollector(ThreadgroupGaugeName,new GaugeCollector(ThreadgroupGaugeName,
				"step thread group active threads count", labelsThreadGroup)
			{
				@Override
				public List<Collector.MetricFamilySamples> collect() {
					return getGauge().collect();
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
		return new MeasurementPlugin(gaugeCollectorRegistry);
	}
}
