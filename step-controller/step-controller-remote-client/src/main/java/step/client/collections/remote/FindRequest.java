package step.client.collections.remote;

import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.SearchOrder;

public class FindRequest {
    Filter filter;
    SearchOrder order;
    Integer skip;
    Integer limit;
    int maxTime;

    public FindRequest(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
        super();
        this.filter = filter;
        this.order = order;
        this.skip = skip;
        this.limit = limit;
        this.maxTime = maxTime;
    }

    public FindRequest() {
        super();
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public SearchOrder getOrder() {
        return order;
    }

    public void setOrder(SearchOrder order) {
        this.order = order;
    }

    public Integer getSkip() {
        return skip;
    }

    public void setSkip(Integer skip) {
        this.skip = skip;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(int maxTime) {
        this.maxTime = maxTime;
    }
}
