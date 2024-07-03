package step.plugins.timeseries.dashboards.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
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
    
    public static class PclColumnSelection extends ColumnSelection {
        
        @NotNull
        private Double pclValue; // can override when column is PCL
        
        public PclColumnSelection() {
            super();
        }
        
        public PclColumnSelection(TableChartColumn column, boolean isSelected, Double pclValue) {
            super(column, isSelected);
            this.pclValue = pclValue;
        }
        
        public Double getPclValue() {
            return pclValue;
        }

        public ColumnSelection setPclValue(Double pclValue) {
            this.pclValue = pclValue;
            return this;
        }
    }

    @JsonSubTypes({
            @JsonSubTypes.Type(value = PclColumnSelection.class)
    })
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class", defaultImpl = ColumnSelection.class)
    public static class ColumnSelection {
        
        @NotNull
        private TableChartColumn column;
        
        @NotNull
        private boolean isSelected;
        
        public ColumnSelection() {
            
        }
        public ColumnSelection(TableChartColumn column, boolean isSelected) {
            this.column = column;
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

