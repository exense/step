package step.artefacts;

import step.core.collections.Filters;
import step.core.dynamicbeans.DynamicValue;

public class Filter {

	public static final String FIELD_FIELD = "field";

	private DynamicValue<String> field;
	private DynamicValue<String> filter;
	private FilterType filterType;
	
	public Filter() {
		super();
	}

	public Filter(String field, String value, FilterType filterType) {
		super();
		this.field = new DynamicValue<String>(field);
		this.filter = new DynamicValue<String>(value);
		this.filterType = filterType;
	}

	public DynamicValue<String> getField() {
		return field;
	}

	public void setField(DynamicValue<String> field) {
		this.field = field;
	}

	public DynamicValue<String> getFilter() {
		return filter;
	}

	public void setFilter(DynamicValue<String> filter) {
		this.filter = filter;
	}

	public FilterType getFilterType() {
		return filterType;
	}
	
	public void setFilterType(FilterType filterType) {
		this.filterType = filterType;
	}

	public step.core.collections.Filter toCollectionFilter() {
		if(filterType == FilterType.REGEX) {
			return Filters.regex(field.get(), filter.get(), true);
		} else {
			return Filters.equals(field.get(), filter.get());
		}
	}
}
