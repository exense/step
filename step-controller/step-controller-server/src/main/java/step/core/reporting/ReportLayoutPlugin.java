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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Plugin
public class ReportLayoutPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(ReportLayoutPlugin.class);

    public static final String DEFAULT_REPORT_LAYOUT = "Default";
    public static final String PRESET_FOLDER_PATH_CONFIG_KEY = "plugins.reporting.layouts.presets.folder";
    public static final String PRESET_FOLDER_PATH_DEFAULT = "../plugins/reporting/layouts";
    /**
     * Comma-separated classpath resource names of the preset layouts. When set, the presets are loaded from these
     * resources instead of the presets folder, as for the Step IDE which is distributed as a single executable file.
     */
    public static final String PRESET_RESOURCES_CONFIG_KEY = "plugins.reporting.layouts.presets.resources";
    public static final String DEFAULT_LAYOUT_ID_CONFIG_KEY = "plugins.reporting.layouts.default.id";
    public static final String DEFAULT_LAYOUT_ID_DEFAULT = "69b010aeec94534eb48176db";
    public static final String CROSS_EXECUTION_DEFAULT_LAYOUT_ID_CONFIG_KEY = "plugins.reporting.layouts.crossexecution.default.id";
    public static final String CROSS_EXECUTION_DEFAULT_LAYOUT_ID_DEFAULT = "6a5a49dfcbff3f2ff375c874";

    private ReportLayoutAccessor reportLayoutAccessor;
    private String defaultLayoutId;
    private String crossExecutionDefaultLayoutId;

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

        //Add default layout IDs (per report type) to UI configuration
        defaultLayoutId = context.getConfiguration().getProperty(DEFAULT_LAYOUT_ID_CONFIG_KEY, DEFAULT_LAYOUT_ID_DEFAULT);
        crossExecutionDefaultLayoutId = context.getConfiguration().getProperty(CROSS_EXECUTION_DEFAULT_LAYOUT_ID_CONFIG_KEY, CROSS_EXECUTION_DEFAULT_LAYOUT_ID_DEFAULT);
        WebApplicationConfigurationManager configurationManager = context.require(WebApplicationConfigurationManager.class);
        configurationManager.registerHook(s -> {
            Map<String, String> config = new HashMap<>();
            config.put(DEFAULT_LAYOUT_ID_CONFIG_KEY, defaultLayoutId);
            config.put(CROSS_EXECUTION_DEFAULT_LAYOUT_ID_CONFIG_KEY, crossExecutionDefaultLayoutId);
            return config;
        });

    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {
        super.initializeData(context);
        // Drop all existing presets - the presets source is the source of truth at startup
        reportLayoutAccessor.getCollectionDriver().remove(
            Filters.equals(ReportLayout.FIELD_VISIBILITY, ReportLayout.ReportLayoutVisibility.Preset.name()));

        String presetResources = context.getConfiguration().getProperty(PRESET_RESOURCES_CONFIG_KEY);
        if (presetResources != null && !presetResources.isBlank()) {
            loadPresetsFromClasspath(presetResources);
        } else {
            loadPresetsFromFolder(new File(context.getConfiguration().getProperty(PRESET_FOLDER_PATH_CONFIG_KEY, PRESET_FOLDER_PATH_DEFAULT)));
        }
    }

    private void loadPresetsFromFolder(File presetsFolder) {
        if (presetsFolder.exists() && presetsFolder.isDirectory()) {
            File[] jsonFiles = presetsFolder.listFiles((dir, name) -> name.endsWith(".json"));
            if (jsonFiles != null) {
                for (File jsonFile : jsonFiles) {
                    try (InputStream inputStream = new FileInputStream(jsonFile)) {
                        loadPreset(inputStream, jsonFile.getAbsolutePath());
                    } catch (Exception e) {
                        logger.error("Failed to load preset layout from file '{}'", jsonFile.getAbsolutePath(), e);
                    }
                }
            }
        } else {
            logger.warn("The configured presets folder '{}' does not exist or is not a directory.", presetsFolder.getAbsolutePath());
        }
    }

    private void loadPresetsFromClasspath(String presetResources) {
        for (String resource : presetResources.split(",")) {
            String resourceName = resource.trim();
            if (resourceName.isEmpty()) {
                continue;
            }
            try (InputStream inputStream = ReportLayoutPlugin.class.getClassLoader().getResourceAsStream(resourceName)) {
                if (inputStream == null) {
                    logger.error("The preset layout resource '{}' could not be found on the classpath", resourceName);
                } else {
                    loadPreset(inputStream, resourceName);
                }
            } catch (Exception e) {
                logger.error("Failed to load preset layout from resource '{}'", resourceName, e);
            }
        }
    }

    private void loadPreset(InputStream inputStream, String source) throws IOException {
        ObjectMapper objectMapper = DefaultJacksonMapperProvider.getObjectMapper();
        ReportLayoutJson layoutJson = objectMapper.readValue(inputStream, ReportLayoutJson.class);
        if (layoutJson == null) {
            logger.error("Invalid json file: {} is empty", source);
            return;
        }
        if (ObjectId.isValid(layoutJson.id)) {
            ReportLayout reportLayout = new ReportLayout(layoutJson.layout, ReportLayout.ReportLayoutVisibility.Preset, layoutJson.reportType);
            reportLayout.addAttribute(AbstractOrganizableObject.NAME, layoutJson.name);
            reportLayout.setId(new ObjectId(layoutJson.id));
            reportLayoutAccessor.save(reportLayout);
        } else {
            logger.error("Invalid json file: {}, the id {} has been tempered with and is not a valid ObjectId", source, layoutJson.id);
        }
    }
}
