package step.plugins.datatable;

import java.util.List;

import step.plugins.datatable.formatters.Formatter;
import step.plugins.screentemplating.InputType;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ColumnDef {

	String title;
	
	String value;
	
	InputType inputType;
	
	@JsonIgnore
	Formatter format;
	
	@JsonIgnore
	SearchQueryFactory queryFactory;
	
	List<String> distinctValues;

	public ColumnDef(String title, String value, InputType inputType,
			Formatter format, SearchQueryFactory queryFactory,
			List<String> distinctValues) {
		super();
		this.title = title;
		this.value = value;
		this.inputType = inputType;
		this.format = format;
		this.queryFactory = queryFactory;
		this.distinctValues = distinctValues;
	}

	public String getTitle() {
		return title;
	}

	public String getValue() {
		return value;
	}

	public Formatter getFormat() {
		return format;
	}

	public void setFormat(Formatter format) {
		this.format = format;
	}

	public List<String> getDistinctValues() {
		return distinctValues;
	}

	public void setDistinctValues(List<String> distinctValues) {
		this.distinctValues = distinctValues;
	}

	public SearchQueryFactory getQueryFactory() {
		return queryFactory;
	}

	public void setQueryFactory(SearchQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	public InputType getInputType() {
		return inputType;
	}

	public void setInputType(InputType inputType) {
		this.inputType = inputType;
	}
	
}
