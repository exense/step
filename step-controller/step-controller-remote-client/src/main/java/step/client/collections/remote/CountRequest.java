package step.client.collections.remote;

import step.core.collections.Filter;

public class CountRequest {

	private Filter filter;
	private Integer limit;

	public CountRequest(Filter filter, Integer limit) {
		super();
		this.filter = filter;
		this.limit = limit;
	}

	public CountRequest() {
		super();
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}
}
