package step.plugins.timeseries.dashboards.model;

import jakarta.validation.constraints.NotNull;
import step.core.timeseries.metric.MetricAttribute;

import java.util.ArrayList;
import java.util.List;

public class TableSettings {
    
    @NotNull
	private String metricKey;
	
	@NotNull
	private List<MetricAttribute> attributes = new ArrayList<>();
    
    @NotNull
	private List<TimeSeriesFilterItem> filters = new ArrayList<>();
    
    private String oql;
    
    @NotNull
    private List<ColumnSelection> columns;

    public String getMetricKey() {
        return metricKey;
    }

    public TableSettings setMetricKey(String metricKey) {
        this.metricKey = metricKey;
        return this;
    }

    public List<MetricAttribute> getAttributes() {
        return attributes;
    }

    public TableSettings setAttributes(List<MetricAttribute> attributes) {
        this.attributes = attributes;
        return this;
    }

    public List<TimeSeriesFilterItem> getFilters() {
        return filters;
    }

    public TableSettings setFilters(List<TimeSeriesFilterItem> filters) {
        this.filters = filters;
        return this;
    }

    public String getOql() {
        return oql;
    }

    public TableSettings setOql(String oql) {
        this.oql = oql;
        return this;
    }

    public List<ColumnSelection> getColumns() {
        return columns;
    }

    public TableSettings setColumns(List<ColumnSelection> columns) {
        this.columns = columns;
        return this;
    }
}

class ColumnSelection {
    @NotNull
    private TableChartColumn column;
    @NotNull
    private boolean isSelected;

    public TableChartColumn getColumn() {
        return column;
    }

    public ColumnSelection setColumn(TableChartColumn column) {
        this.column = column;
        return this;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public ColumnSelection setSelected(boolean selected) {
        isSelected = selected;
        return this;
    }
}
