package step.plugins.measurements;

import io.prometheus.client.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.grid.GridPlugin;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.grid.Grid;
import step.grid.GridReportBuilder;
import step.grid.TokenWrapperState;
import step.grid.reports.TokenGroupCapacity;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.plugins.measurements.MeasurementPlugin.*;

@Plugin(dependencies = GridPlugin.class)
public class MeasurementControllerPlugin extends AbstractControllerPlugin {

	private static Logger logger = LoggerFactory.getLogger(MeasurementControllerPlugin.class);
	public static String ThreadgroupGaugeName = "step_threadgroup";

	GaugeCollectorRegistry gaugeCollectorRegistry;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		initGaugeCollectorRegistry(context);
	}

	protected void initGaugeCollectorRegistry(GlobalContext context) {
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
		int interval = context.getConfiguration().getPropertyAsInteger("plugins.measurements.gaugecollector.interval",15);
		gaugeCollectorRegistry.start(interval);
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		gaugeCollectorRegistry.stop();
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new MeasurementPlugin(gaugeCollectorRegistry);
	}
}
