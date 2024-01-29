package step.artefacts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.ContainsDynamicValues;
import step.core.dynamicbeans.DynamicValue;

@Artefact
public class PerformanceAssert extends AbstractArtefact {

	private List<Filter> filters;
	private Aggregator aggregator = Aggregator.AVG;
	private Comparator comparator = Comparator.LOWER_THAN;
	private DynamicValue<Number> expectedValue = new DynamicValue<Number>(3000l);
	
	public PerformanceAssert() {
		super();
	}

	public PerformanceAssert(Aggregator aggregator, Comparator comparator, Number expectedValue, Filter... filters) {
		super();
		this.filters = new ArrayList<>(Arrays.asList(filters));
		this.aggregator = aggregator;
		this.comparator = comparator;
		this.expectedValue = new DynamicValue<>(expectedValue);
	}
	
	public PerformanceAssert(List<Filter> filters, Aggregator aggregator, Comparator comparator, Number expectedValue) {
		super();
		this.filters = filters;
		this.aggregator = aggregator;
		this.comparator = comparator;
		this.expectedValue = new DynamicValue<>(expectedValue);
	}

	@Override
	public boolean isPropertyArefact() {
		return true;
	}

	@ContainsDynamicValues
	public List<Filter> getFilters() {
		return filters;
	}

	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	public Aggregator getAggregator() {
		return aggregator;
	}

	public void setAggregator(Aggregator aggregator) {
		this.aggregator = aggregator;
	}

	public Comparator getComparator() {
		return comparator;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	public DynamicValue<Number> getExpectedValue() {
		return expectedValue;
	}

	public void setExpectedValue(DynamicValue<Number> expectedValue) {
		this.expectedValue = expectedValue;
	}

}
