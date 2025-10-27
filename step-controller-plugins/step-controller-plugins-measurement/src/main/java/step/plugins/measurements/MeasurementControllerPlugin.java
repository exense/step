package step.plugins.measurements;

import io.prometheus.client.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.grid.GridPlugin;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.entities.EntityManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.grid.Grid;
import step.grid.GridReportBuilder;
import step.grid.TokenWrapperState;
import step.grid.reports.TokenGroupCapacity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.plugins.measurements.MeasurementPlugin.*;

@Plugin(dependencies = {GridPlugin.class})
public class MeasurementControllerPlugin extends AbstractControllerPlugin {

	private static Logger logger = LoggerFactory.getLogger(MeasurementControllerPlugin.class);

	public static String GridGaugeName = "step_grid_tokens";
	public static String ThreadgroupGaugeName = "step_threadgroup";
	public static String ReportMeasurementsTableName = "reportMeasurements";
	private GaugeCollectorRegistry gaugeCollectorRegistry;

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		super.serverStart(context);

		Collection<Measurement> collection = context.getCollectionFactory().getCollection(EntityManager.measurements, Measurement.class);
		context.require(TableRegistry.class).register(ReportMeasurementsTableName,
				new Table<>(collection, null, false)
						.withResultItemTransformer((m, session) -> convertToPseudoMeasure(m))
		);

		initGaugeCollectorRegistry(context);
	}

	/*
	 This will convert a "full", flattened, measurement back
	 to a format that's structurally identical to a measure.
	 IOW, the (JSON) serialized result should "almost" be deserializable back to a Measure,
	 but it may contain status values that come from a ReportNode instead of a Measure, and
	 therefore might be invalid as a Measure.Status enum value. The frontend (who is the
	 only intended user of this) knows how to handle these objects.
	 */
	private Measurement convertToPseudoMeasure(Measurement in) {
		Measurement out = new Measurement();
		Map<String, Object> data = new HashMap<>();
		for (Map.Entry<String, Object> entry : in.entrySet()) {
			String key = entry.getKey();
			if (MEASURE_FIELDS.contains(key)) {
				if (key.equals(VALUE)) {
					// special case: map "value" back to "duration"
					out.put("duration", entry.getValue());
				} else {
					out.put(key, entry.getValue());
				}
			} else if (!MEASURE_NOT_DATA_KEYS.contains(key)) {
				data.put(key, entry.getValue());
			}
		}
		if (!data.isEmpty()) {
			out.put("data", data);
		}
		return out;
	}

	protected void initGaugeCollectorRegistry(GlobalContext context) {
		gaugeCollectorRegistry = GaugeCollectorRegistry.getInstance();
		if (context.get(Grid.class)!=null) {
			//Register grid gauge metrics
			//Define grouping attributes and corresponds metric labels ($ not supported)
			List<String> groupBy = new ArrayList<>(Arrays.asList(context.getConfiguration()
					.getProperty("plugins.grid.monitoring.attributes", "$agenttype,type")
					.split(",")));
			if (!groupBy.contains("url")) {
				groupBy.add("url");
			}
			List<String> labels = groupBy.stream().map(s->s.replace("$","")).collect(Collectors.toList());
			labels.add("name");
			//Project id not added yet to the grid metrics
			gaugeCollectorRegistry.registerCollector(GridGaugeName, new GaugeCollector(GridGaugeName,
					"step grid token usage and capacity", labels.stream().toArray(String[]::new)) {
				final GridReportBuilder gridReportBuilder = new GridReportBuilder((Grid) context.get(Grid.class));
				final Map<String, String[][]> urlToType = new ConcurrentHashMap<>();

				@Override
				public List<Collector.MetricFamilySamples> collect() {
					List<TokenGroupCapacity> usageByIdentity = gridReportBuilder.getUsageByIdentity(groupBy);
					Map<String, String[][]> newUrlToType = new HashMap<>();
					for (TokenGroupCapacity tokenGroupCapacity : usageByIdentity) {
						Integer free = tokenGroupCapacity.getCountByState().get(TokenWrapperState.FREE);
						free = (free == null) ? 0 : free;
						int capacity = tokenGroupCapacity.getCapacity();
						String agentUrl = tokenGroupCapacity.getKey().get("url");
						urlToType.remove(agentUrl);
						List<String> labelValues = groupBy.stream().map(s -> Objects.requireNonNullElse(tokenGroupCapacity.getKey().get(s), "")).collect(Collectors.toList());
						String[] lblValFree = Stream.concat(labelValues.stream(),Stream.of("free")).toArray(String[]::new);
						String[] lblValCapacity = Stream.concat(labelValues.stream(),Stream.of("capacity")).toArray(String[]::new);
						getGauge().labels(lblValFree).set(Double.valueOf(free));
						getGauge().labels(lblValCapacity).set(Double.valueOf(capacity));
						newUrlToType.put(agentUrl, new String[][]{lblValFree,lblValCapacity});
					}
					//Remaining urls from previous iteration doesn't exist anymore, cleaning-up metrics
					for (String url : urlToType.keySet()) {
						String[][] labelsAr = urlToType.remove(url);
						for (int i=0; i < labelsAr.length; i++) {
							getGauge().remove(labelsAr[i]);
						}
					}
					//save set of agent urls from current iteration
					urlToType.putAll(newUrlToType);
					return getGauge().collect();
				}
			});
		} else {
			logger.warn("No grid instance found in context, the measurements of the grid token usage will be disabled");
		}

		gaugeCollectorRegistry = GaugeCollectorRegistry.getInstance();


		//Start the gauge scheduler
		int interval = context.getConfiguration().getPropertyAsInteger("plugins.measurements.gaugecollector.interval",15);
		gaugeCollectorRegistry.start(interval);
	}

	@Override
	public void serverStop(GlobalContext context) {
		gaugeCollectorRegistry.stop();
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new MeasurementPlugin(gaugeCollectorRegistry);
	}
}
