package step.plugins.measurements;

import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;
import step.core.reports.MetricSampleType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static step.plugins.measurements.MeasurementPlugin.THREAD_GROUP;

/**
 * Base class for gauge-based metric collectors used by {@link GaugeCollectorRegistry}.
 *
 * <h3>Role in the measurement pipeline</h3>
 * <p>A {@code GaugeCollector} owns a named Prometheus {@link Gauge} and knows how to read its
 * current label-value samples via the abstract {@link #collect()} method. On each sampling
 * tick triggered by the {@link GaugeCollectorRegistry}, {@link #collectAsMeasurements()} is
 * called to convert those Prometheus samples into {@link Measurement} objects (with
 * {@code metricType = GAUGE}) that are then forwarded to all registered
 * {@link MeasurementHandler}s (time-series ingestion, raw storage, Prometheus export, …).
 *
 * <h3>Typical uses</h3>
 * <ul>
 *   <li><b>Thread-group concurrency</b> — tracks the number of active virtual users inside a
 *       running {@code ThreadGroup} artefact. The gauge is incremented/decremented as threads
 *       enter and exit, and the periodic snapshots ensure the chart always shows the correct
 *       concurrency level even in quiet time-windows.</li>
 *   <li><b>Platform self-monitoring</b> — agent token pool utilisation and other Step-internal
 *       resource metrics are exposed as gauges so that operations teams can track them over
 *       time via the same time-series infrastructure used for test results.</li>
 * </ul>
 *
 * <h3>Implementing a custom collector</h3>
 * <p>Subclass {@code GaugeCollector}, provide a meaningful {@code name}, {@code description},
 * and {@code labels} array, then implement {@link #collect()} to return the current Prometheus
 * samples. Register the instance with
 * {@link GaugeCollectorRegistry#registerCollector(String, GaugeCollector)}.
 */
public abstract class GaugeCollector {
    public String name;
    public String description;
    public String[] labels;

    /** Date formatter used by some collectors to include a date dimension in label values. */
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public GaugeCollector(String name, String description, String[] labels) {
        this.name = name;
        this.description = description;
        this.labels = labels;
    }

    /**
     * Returns (or lazily creates) the Prometheus {@link Gauge} for this collector.
     */
    public Gauge getGauge() {
        return PrometheusCollectorRegistry.getInstance().getOrCreateGauge(name, description,
            labels);
    }

    /**
     * Converts the current Prometheus samples returned by {@link #collect()} into
     * {@link Measurement} objects ready for dispatch to {@link MeasurementHandler}s.
     * <p>Each sample becomes one {@link Measurement} with:
     * <ul>
     *   <li>{@code metricType} = {@link step.core.reports.MetricSampleType#GAUGE}</li>
     *   <li>{@code type} = the sample name with the {@code step_} prefix stripped</li>
     *   <li>{@code begin} = the sample timestamp (or {@code System.currentTimeMillis()} if absent)</li>
     *   <li>one entry per Prometheus label name/value pair</li>
     *   <li>{@code value} = the raw sample value as a {@code double}</li>
     * </ul>
     */
    public List<Measurement> collectAsMeasurements() {
        List<Collector.MetricFamilySamples> metrics = this.collect();
        List<Measurement> measurements = new ArrayList<>();
        for (Collector.MetricFamilySamples metric : metrics) {
            for (Collector.MetricFamilySamples.Sample sample : metric.samples) {
                Measurement measurement = new Measurement();
                //prefix is only required for prometheus
                String type = sample.name.replaceFirst("step_", "");
                measurement.setType(type);
                //threadgroup is a special case and have is own metricType
                if (THREAD_GROUP.equals(type)) {
                  measurement.setMetricType(type);
                } else {
                    measurement.setMetricType(MetricSampleType.GAUGE.value());
                }
                long ts = (sample.timestampMs != null) ? sample.timestampMs : System.currentTimeMillis();
                measurement.setBegin(ts);
                for (int i = 0; i < sample.labelNames.size(); i++) {
                    measurement.put(sample.labelNames.get(i), sample.labelValues.get(i));
                }
                measurement.setValue(sample.value);
                measurements.add(measurement);
            }
        }
        return measurements;
    }

    /**
     * Returns the current Prometheus metric family samples for this gauge.
     * Implementations should read the gauge's current label-value combinations and return
     * them as a list of {@link io.prometheus.client.Collector.MetricFamilySamples}.
     */
    abstract public List<Collector.MetricFamilySamples> collect();

}
