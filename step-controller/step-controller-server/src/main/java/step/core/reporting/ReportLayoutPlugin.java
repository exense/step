package step.core.reporting;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import step.core.GlobalContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;

import java.io.InputStream;

@Plugin
public class ReportLayoutPlugin extends AbstractControllerPlugin {

    public static final String DEFAULT_REPORT_LAYOUT = "Default";

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);
        TableRegistry tableRegistry = context.require(TableRegistry.class);
        //Create accessor
        Collection<ReportLayout> reportLayoutCollection = context.getCollectionFactory().getCollection(EntityConstants.reportLayouts, ReportLayout.class);
        ReportLayoutAccessor reportLayoutAbstractAccessor = new ReportLayoutAccessor(reportLayoutCollection);
        context.put(ReportLayoutAccessor.class, reportLayoutAbstractAccessor);
        //Register entity
        context.getEntityManager().register(new Entity<>(EntityConstants.reportLayouts, reportLayoutAbstractAccessor, ReportLayout.class));
        //Register Table, table only return layout metadata not the layout itself
        tableRegistry.register(EntityConstants.reportLayouts, new Table<>(reportLayoutCollection, ReportLayoutServices.REPORT_LAYOUT_RIGHT + "-read", false).withResultItemTransformer((reportLayout, session) -> {
            reportLayout.layout = null;
            return reportLayout;
        }));
        //Register Services
        context.getServiceRegistrationCallback().registerService(ReportLayoutServices.class);
    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {
        super.initializeData(context);
        ReportLayoutAccessor reportLayoutAccessor = context.require(ReportLayoutAccessor.class);
        ReportLayout existingDefaultLayout = reportLayoutAccessor.getReportLayoutPresetIfExists(DEFAULT_REPORT_LAYOUT);
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream("presets/DefaultLayout.json");
             JsonReader reader = Json.createReader(resourceAsStream)) {
            JsonObject layout = reader.readObject();
            ReportLayout reportLayout = new ReportLayout(layout, ReportLayout.ReportLayoutVisibility.Preset);
            reportLayout.addAttribute(AbstractOrganizableObject.NAME, DEFAULT_REPORT_LAYOUT);
            //Keeping same strategy as for the prepopulated dashboard, always recreate and update the existing persisted layout
            if (existingDefaultLayout != null) {
                reportLayout.setId(existingDefaultLayout.getId());
            }
            reportLayoutAccessor.save(reportLayout);
        }


    }
}
