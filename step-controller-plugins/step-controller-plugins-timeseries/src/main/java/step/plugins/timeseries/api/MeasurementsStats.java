package step.plugins.timeseries.api;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MeasurementsStats {
	
	@NotNull
	private long count;
	@NotNull
	private Set<String> attributes;
	
	// attribute - values percentages
	private Map<String, List<AttributeStats>> values;

	public MeasurementsStats(long count, Set<String> attributes, Map<String, List<AttributeStats>> values) {
		this.count = count;
		this.attributes = attributes;
		this.values = values;
	}

	public long getCount() {
		return count;
	}

	public Set<String> getAttributes() {
		return attributes;
	}

	public Map<String, List<AttributeStats>> getValues() {
		return values;
	}
}
