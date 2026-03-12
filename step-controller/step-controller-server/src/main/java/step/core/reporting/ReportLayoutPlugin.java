package step.core.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.reporting.model.ReportLayout;
import step.core.reporting.model.ReportLayoutJson;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Plugin
public class ReportLayoutPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(ReportLayoutPlugin.class);

    public static final String DEFAULT_REPORT_LAYOUT = "Default";
    public static final String PRESET_FOLDER_PATH_CONFIG_KEY = "plugins.reporting.layouts.presets.folder";
    public static final String PRESET_FOLDER_PATH_DEFAULT = "../plugins/reporting/layouts";
    public static final String DEFAULT_LAYOUT_ID_CONFIG_KEY = "plugins.reporting.layouts.default.id";
    public static final String DEFAULT_LAYOUT_ID_DEFAULT = "69b010aeec94534eb48176db";

    private ReportLayoutAccessor reportLayoutAccessor;
    private String defaultLayoutId;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);
        TableRegistry tableRegistry = context.require(TableRegistry.class);
        //Create accessor
        Collection<ReportLayout> reportLayoutCollection = context.getCollectionFactory().getCollection(EntityConstants.reportLayouts, ReportLayout.class);
        reportLayoutAccessor = new ReportLayoutAccessor(reportLayoutCollection);
        context.put(ReportLayoutAccessor.class, reportLayoutAccessor);
        //Register entity
        context.getEntityManager().register(new Entity<>(EntityConstants.reportLayouts, reportLayoutAccessor, ReportLayout.class));
        //Register Table, table only return layout metadata not the layout itself
        tableRegistry.register(EntityConstants.reportLayouts, new Table<>(reportLayoutCollection, ReportLayoutServices.REPORT_LAYOUT_RIGHT + "-read", false).withResultItemTransformer((reportLayout, session) -> {
            reportLayout.layout = null;
            return reportLayout;
        }));
        //Register Services
        context.getServiceRegistrationCallback().registerService(ReportLayoutServices.class);

        //Add default layout ID to UI configuration
        defaultLayoutId = context.getConfiguration().getProperty(DEFAULT_LAYOUT_ID_CONFIG_KEY, DEFAULT_LAYOUT_ID_DEFAULT);
        WebApplicationConfigurationManager configurationManager = context.require(WebApplicationConfigurationManager.class);
        configurationManager.registerHook(s -> Map.of(DEFAULT_LAYOUT_ID_CONFIG_KEY, defaultLayoutId));

    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {
        super.initializeData(context);
        // Drop all existing presets - the folder is the source of truth at startup
        reportLayoutAccessor.getCollectionDriver().remove(
                Filters.equals(ReportLayout.FIELD_VISIBILITY, ReportLayout.ReportLayoutVisibility.Preset.name()));

        // Load presets from the configured folder
        File presetsFolder = new File(context.getConfiguration().getProperty(PRESET_FOLDER_PATH_CONFIG_KEY, PRESET_FOLDER_PATH_DEFAULT));
        if (presetsFolder.exists() && presetsFolder.isDirectory()) {
            ObjectMapper objectMapper = DefaultJacksonMapperProvider.getObjectMapper();
            File[] jsonFiles = presetsFolder.listFiles((dir, name) -> name.endsWith(".json"));
            if (jsonFiles != null) {
                for (File jsonFile : jsonFiles) {
                    try {
                        ReportLayoutJson layoutJson = objectMapper.readValue(jsonFile, ReportLayoutJson.class);
                        if (ObjectId.isValid(layoutJson.id)) {
                            ReportLayout reportLayout = new ReportLayout(layoutJson.layout, ReportLayout.ReportLayoutVisibility.Preset);
                            reportLayout.addAttribute(AbstractOrganizableObject.NAME, layoutJson.name);
                            reportLayout.setId(new ObjectId(layoutJson.id));
                            reportLayoutAccessor.save(reportLayout);
                        } else {
                            logger.error("Invalid json file: {}, the id {} has been tempered with and is not a valid ObjectId", jsonFile.getAbsolutePath(),  layoutJson.id);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to load preset layout from file '{}'", jsonFile.getAbsolutePath(), e);
                    }
                }
            }
        } else {
            logger.warn("The configured presets folder '{}' does not exist or is not a directory.", presetsFolder.getAbsolutePath());
        }
    }
}
