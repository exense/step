package step.plugins.measurements;

import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public abstract class GaugeCollector {
	public String name;
	public String description;
	public String[] labels;

	public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

	public GaugeCollector(String name, String description, String[] labels) {
		this.name = name;
		this.description = description;
		this.labels = labels;
	}

	public Gauge getGauge() {
		return PrometheusCollectorRegistry.getInstance().getOrCreateGauge(name, description,
				labels);
	}

	public List<Measurement> collectAsMeasurements() {
		List<Collector.MetricFamilySamples> metrics = this.collect();
		List<Measurement> measurements = new ArrayList<>();
		for (Collector.MetricFamilySamples metric : metrics) {
			for (Collector.MetricFamilySamples.Sample sample : metric.samples) {
				Measurement measurement = new Measurement();
				//prefix is only required for prometheus
				String type = sample.name.replaceFirst("step_","");
				measurement.setType(type);
				long ts = (sample.timestampMs != null) ? sample.timestampMs : System.currentTimeMillis();
				measurement.setBegin(ts);
				for (int i = 0; i < sample.labelNames.size(); i++) {
					measurement.put(sample.labelNames.get(i), sample.labelValues.get(i));
				}
				measurement.setValue(Math.round(sample.value));
				measurements.add(measurement);
			}
		}
		return measurements;
	}

	abstract public List<Collector.MetricFamilySamples> collect();

}
