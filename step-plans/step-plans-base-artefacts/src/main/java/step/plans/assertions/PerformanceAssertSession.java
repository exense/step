package step.plans.assertions;

import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import step.artefacts.Aggregation;
import step.core.reports.Measure;

public class PerformanceAssertSession {

	private final ConcurrentHashMap<String, Aggregation> aggregations = new ConcurrentHashMap<>();
	
	public final void addMeasure(Measure measure) {
		Aggregation aggregation = aggregations.computeIfAbsent(measure.getName(), name->new Aggregation(name));
		aggregation.addMeasure(measure);
	}

	public Set<Entry<String, Aggregation>> getAggregations() {
		return aggregations.entrySet();
	}
}
