package step.plugins.timeseries.migration;

import step.core.Version;
import step.core.collections.*;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static step.controller.services.entities.AbstractEntityServices.CUSTOM_FIELD_LOCKED;

public class MigrateAggregateTask extends MigrationTask {

    private final Collection<Document> dashboardsCollection;
    private final Collection<Document> metricsCollection;

    public MigrateAggregateTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3, 26, 0), collectionFactory, migrationContext);

        dashboardsCollection = collectionFactory.getCollection("dashboards", Document.class);
        metricsCollection = collectionFactory.getCollection("metricTypes", Document.class);
    }

    @Override
    public void runUpgradeScript() {
        System.out.println("RUNNING MIGRATION SCRIPT");
        updateMetricTypes();
        updateCustomDashboards();
    }

    @Override
    public void runDowngradeScript() {

    }
    
    private void updateMetricTypes() {
        metricsCollection.find(Filters.empty(), null, null, null, 0).forEach(metric -> {
            String aggregation = metric.getString("defaultAggregation");
            metric.put("defaultAggregation", transformAggregation(aggregation, null));
            metricsCollection.save(metric);
        });
    }
    
     private void updateCustomDashboards() {
        // ignore generated dashboards
        dashboardsCollection.find(Filters.not(Filters.equals(CUSTOM_FIELD_LOCKED, true)), null, null, null, 0).forEach(dashboard -> {
            List<DocumentObject> dashlets = dashboard.getArray("dashlets");
            dashlets.forEach(dashlet -> {
                String dashletType = dashlet.getString("type");
                switch (dashletType) {
                    case "CHART":
                        DocumentObject chartSettings = dashlet.getObject("chartSettings");
                        updateAxesSettings(chartSettings.getObject("primaryAxes"));
                        updateAxesSettings(chartSettings.getObject("secondaryAxes"));
                        break;
                    case "TABLE":
                        DocumentObject tableSettings = dashlet.getObject("tableSettings");
                        updateTableSettings(tableSettings);
                        break;
                    default: throw new IllegalStateException("Invalid dashlet type found: " + dashletType);
                }

            });
            dashboardsCollection.save(dashboard);
        });
    }
    
    private void updateAxesSettings(DocumentObject axesSettings) {
        if (axesSettings == null) {
            return;
        }
        Object oldPclValue = axesSettings.get("pclValue");
        axesSettings.remove("pclValue");
        DocumentObject newAggregation = new Document();
        String oldAggregation = axesSettings.getString("aggregation");
        axesSettings.put("aggregation", transformAggregation(oldAggregation, oldPclValue));
        
    }
    
    private DocumentObject transformAggregation(String oldAggregation, Object oldPclValue) {
        DocumentObject newAggregation = new Document();
        newAggregation.put("type", oldAggregation);
        if (Objects.equals(oldAggregation, "PERCENTILE")) {
            newAggregation.put("params", Map.of("pclValue", oldPclValue != null ? oldPclValue : 90));
        }
        if (Objects.equals(oldAggregation, "RATE")) {
            newAggregation.put("params", Map.of("rateUnit", "h"));
        }
        return newAggregation;
    }
    
    private void updateTableSettings(DocumentObject tableDashletSettings) {
        List<DocumentObject> columns = tableDashletSettings.getArray("columns");
        columns.forEach(columnSelection -> {
            String columnType = columnSelection.getString("column");
            switch (columnType) {
                case "PCL_80":
                    columnSelection.put("column", "PCL_1");
                    break;
                case "PCL_90":
                    columnSelection.put("column", "PCL_2");
                    break;
                case "PCL_99":
                    columnSelection.put("column", "PCL_3");
                    break;
            }
            columnSelection.put("aggregation", getAggregationForTableColumn(columnType));
        });
    }
    
    private Document getAggregationForTableColumn(String column) {
        String aggregationType;
        Document params = null;
        switch (column) {
            case "COUNT":
            case "SUM":
            case "AVG":
            case "MIN":
            case "MAX":
                aggregationType = column;
                break;
            case "PCL_80":
                aggregationType = "PERCENTILE";
                params = new Document(Map.of("pclValue", 80));
                break;
            case "PCL_90":
                aggregationType = "PERCENTILE";
                params = new Document(Map.of("pclValue", 90));
                break;
            case "PCL_99":
                aggregationType = "PERCENTILE";
                params = new Document(Map.of("pclValue", 99));
                break;
            case "TPS":
                aggregationType = "RATE";
                params = new Document(Map.of("rateUnit", "s"));
                break;
            case "TPH":
                aggregationType = "RATE";
                params = new Document(Map.of("rateUnit", "h"));
                break;
            default:
                throw new IllegalStateException("Invalid column: " + column);
        }
        Document finalAggregate = new Document();
        finalAggregate.put("type", aggregationType);
        if (params != null) {
            finalAggregate.put("params", params);
        }
        return finalAggregate;
    }
    
    
}
