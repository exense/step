package step.artefacts;

import java.util.function.BiFunction;

public enum Comparator {

	EQUALS((v1,v2)->v1.doubleValue()==v2.doubleValue(), "equals to", "equal to"),
	HIGHER_THAN((v1,v2)->v1.doubleValue()>v2.doubleValue(), "higher than", "higher than"),
	LOWER_THAN((v1,v2)->v1.doubleValue()<v2.doubleValue(), "lower than", "lower than");
	
	private final BiFunction<Number, Number, Boolean> comparatorFunction;
	private final String description;
	private final String errorDescription;

	Comparator(BiFunction<Number, Number, Boolean> comparatorFunction, String description, String errorDescription) {
		this.comparatorFunction = comparatorFunction;
		this.description = description;
		this.errorDescription = errorDescription;
	}

	public BiFunction<Number, Number, Boolean> getComparatorFunction() {
		return comparatorFunction;
	}

	public String getDescription() {
		return description;
	}

	public String getErrorDescription() {
		return errorDescription;
	}
}
