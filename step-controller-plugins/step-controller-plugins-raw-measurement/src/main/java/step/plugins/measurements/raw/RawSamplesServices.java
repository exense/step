package step.plugins.measurements.raw;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.core.deployment.AbstractStepServices;
import step.core.metrics.MetricSample;
import step.core.metrics.MetricType;
import step.framework.server.security.Secured;
import step.plugins.measurements.StepMetricSample;

import java.util.*;
import java.util.stream.Stream;

@Singleton
@Path("raw-samples")
@Tag(name = "RawSamples")
public class RawSamplesServices extends AbstractStepServices {

    private MetricSampleAccessor metricSampleAccessor;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        metricSampleAccessor = getContext().require(MetricSampleAccessor.class);
    }

    public RawSamplesServices() {
    }

    // for unit testing only
    RawSamplesServices(MetricSampleAccessor metricSampleAccessor) {
        this.metricSampleAccessor = metricSampleAccessor;
    }

    @Operation(description = "Returns the aggregated metric samples for the provided report node id")
    @GET
    @Path("/metric-samples/{rnId}/aggregated")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "execution-read")
    public List<MetricSample> getAggregatedMetricSamples(@PathParam("rnId") String rnId) {
        // Registry: one aggregated MetricSample per unique (name, labels) combination
        Map<String, MetricSample> registry = new LinkedHashMap<>();
        try (Stream<StepMetricSample> stream = metricSampleAccessor.findByReportNodeId(rnId)) {
            stream.forEach(stepMetricSample -> {
                MetricSample sample = stepMetricSample.sample;
                String key = sample.getName() + "|" + new TreeMap<>(sample.getLabels());
                registry.merge(key, sample, RawSamplesServices::mergeSamples);
            });
        }
        return new ArrayList<>(registry.values());
    }

    /**
     * Merges two {@link MetricSample}s of the same metric (same name + labels) into one.
     * <p>
     * Counter semantics: {@code count} accumulates the per-interval diffs;
     * {@code sum/min/max/last} reflect the latest running total (from the more recent sample).
     * <p>
     * Gauge/Histogram semantics: all statistical fields are combined
     * ({@code count}, {@code sum} summed; {@code min}/{@code max} extremes kept;
     * distribution buckets merged; {@code last} taken from the more recent sample).
     */
    static MetricSample mergeSamples(MetricSample existing, MetricSample incoming) {
        long sampleTime = Math.max(existing.getSampleTime(), incoming.getSampleTime());
        long count = existing.getCount() + incoming.getCount();
        long sum, min, max, last;
        Map<Long, Long> distribution;
        if (existing.getType() == MetricType.COUNTER) {
            // Running total comes from the most recent sample's sum field
            long runningTotal = incoming.getSampleTime() >= existing.getSampleTime()
                    ? incoming.getSum() : existing.getSum();
            sum = runningTotal;
            min = runningTotal;
            max = runningTotal;
            last = runningTotal;
            distribution = null;
        } else {
            sum = existing.getSum() + incoming.getSum();
            min = Math.min(existing.getMin(), incoming.getMin());
            max = Math.max(existing.getMax(), incoming.getMax());
            last = incoming.getSampleTime() >= existing.getSampleTime()
                    ? incoming.getLast() : existing.getLast();
            distribution = mergeDistributions(existing.getDistribution(), incoming.getDistribution());
        }
        return new MetricSample(sampleTime, existing.getName(), existing.getLabels(),
                existing.getType(), count, sum, min, max, last, distribution);
    }

    static Map<Long, Long> mergeDistributions(Map<Long, Long> a, Map<Long, Long> b) {
        if (a == null && b == null) return null;
        Map<Long, Long> result = new HashMap<>(a != null ? a : Collections.emptyMap());
        if (b != null) {
            b.forEach((bucket, bucketCount) -> result.merge(bucket, bucketCount, Long::sum));
        }
        return result;
    }
}
