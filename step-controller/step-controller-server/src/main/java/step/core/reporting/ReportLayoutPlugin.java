package step.core.reporting;

import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;

import java.util.Map;

@Plugin
public class ReportLayoutPlugin extends AbstractControllerPlugin {

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
            reportLayout.layout = Map.of();
            return reportLayout;
        }));
        //Register Services
        context.getServiceRegistrationCallback().registerService(ReportLayoutServices.class);
    }
}
