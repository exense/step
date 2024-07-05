package step.plugins.timeseries.dashboards.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import step.core.timeseries.metric.MetricAggregation;
import step.plugins.table.settings.ScreenInputColumnSettings;

import java.util.List;

import static step.functions.Function.JSON_CLASS_FIELD;

public class TableDashletSettings {
    
    @NotNull
    private List<ColumnSelection> columns;

    public List<ColumnSelection> getColumns() {
        return columns;
    }

    public TableDashletSettings setColumns(List<ColumnSelection> columns) {
        this.columns = columns;
        return this;
    }
    
    public static class ColumnSelection {
        
        @NotNull
        private TableChartColumn column;
        
        private MetricAggregation aggregation;
        
        @NotNull
        private boolean isSelected = true;
        
        public ColumnSelection() {
            
        }
        
        public ColumnSelection(TableChartColumn column, MetricAggregation aggregation) {
            this.column = column;
            this.aggregation = aggregation;
        }
        public ColumnSelection(TableChartColumn column, MetricAggregation aggregation, boolean isSelected) {
            this.column = column;
            this.aggregation = aggregation;
            this.isSelected = isSelected;
        }

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

}

