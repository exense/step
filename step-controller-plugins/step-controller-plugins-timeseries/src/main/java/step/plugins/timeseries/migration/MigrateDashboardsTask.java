package step.plugins.timeseries.migration;

import org.apache.commons.lang3.RandomStringUtils;
import step.core.Version;
import step.core.collections.*;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.Arrays;
import java.util.List;

public class MigrateDashboardsTask extends MigrationTask {

    private static final List<String> ATTRIBUTES_TO_MIGRATE = Arrays.asList(
            "metricKey", "attributes", "filters", "oql", "grouping", 
            "inheritGlobalFilters",
            "inheritGlobalGrouping",
            "readonlyGrouping",
            "readonlyAggregate");
    
    private final Collection<Document> dashboardsCollection;
    
    public MigrateDashboardsTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3,25,0), collectionFactory, migrationContext);
        
        dashboardsCollection = collectionFactory.getCollection("dashboards", Document.class);
    }

    @Override
    public void runUpgradeScript() {
        cleanupGrafanaDashboard();
        migrateNamePropertyToAttributes();
        migrateCustomDashboards();
    }

    private void cleanupGrafanaDashboard() {
        dashboardsCollection.remove(Filters.exists("uid"));
    }

    private void migrateNamePropertyToAttributes() {
        dashboardsCollection.find(Filters.empty(), null, null, null, 0).forEach(dashboard -> {
            DocumentObject attributes = dashboard.getObject("attributes");
            if (attributes == null) {
                attributes = new DocumentObject();
                dashboard.put("attributes", attributes);
            }
            String name = (String) dashboard.remove("name");
            attributes.put("name", name);
            dashboardsCollection.save(dashboard);
        });
    }

    private void migrateCustomDashboards() {
        // keep legacy dashboard unchanged
        Filter notLegacyFilter = Filters.not(Filters.equals("metadata.isLegacy", true));
        dashboardsCollection.find(notLegacyFilter, null, null, null, 0).forEach(dashboard -> {
            List<DocumentObject> dashlets = dashboard.getArray("dashlets");
            dashlets.forEach(dashlet -> {
                dashlet.put("id", RandomStringUtils.randomAlphanumeric(10)); // dashlets must have an id

                DocumentObject chartSettings = dashlet.getObject("chartSettings");

                ATTRIBUTES_TO_MIGRATE.forEach(attribute -> {
                    dashlet.put(attribute, chartSettings.get(attribute));
                    chartSettings.remove(attribute);
                });
            });
            dashboardsCollection.save(dashboard);
        });
    }

    @Override
    public void runDowngradeScript() {

    }
}
