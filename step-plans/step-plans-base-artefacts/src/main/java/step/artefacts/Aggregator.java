package step.artefacts;

import java.util.function.Function;

public enum Aggregator {
	
	AVG(Aggregation::getAvg, "Average"),
	MAX(Aggregation::getMax, "Max"),
	MIN(Aggregation::getMin, "Min"),
	COUNT(Aggregation::getCount, "Count"),
	SUM(Aggregation::getSum, "Sum");

	private Function<Aggregation, Number> valueFunction;
	private String description;

	private Aggregator(Function<Aggregation, Number> supplier, String description) {
		this.valueFunction = supplier;
		this.description = description;
	}

	public Function<Aggregation, Number> getValueFunction() {
		return valueFunction;
	}

	public String getDescription() {
		return description;
	}
}
