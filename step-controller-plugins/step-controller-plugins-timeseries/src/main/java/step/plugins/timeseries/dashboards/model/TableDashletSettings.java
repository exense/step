package step.plugins.timeseries.dashboards.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;

import java.util.List;

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
        @NotNull
        private boolean isSelected;
        
        public ColumnSelection() {}
        
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

