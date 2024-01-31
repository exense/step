package step.artefacts;

import java.util.function.BiFunction;

public enum FilterType {

	EQUALS(String::equals),
	REGEX(String::matches);
	
	private final BiFunction<String, String, Boolean> filterFunction;

	private FilterType(BiFunction<String, String, Boolean> filterFunction) {
		this.filterFunction = filterFunction;
	}

	public BiFunction<String, String, Boolean> getFilterFunction() {
		return filterFunction;
	}
}
