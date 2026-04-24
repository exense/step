/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.controller.grid;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.grid.services.GridServices;
import step.core.GlobalContext;
import step.core.metrics.ControllerMetricSample;
import step.core.metrics.InstrumentType;
import step.core.metrics.MetricSample;
import step.core.metrics.MetricSampler;
import step.core.metrics.MetricSamplerRegistry;
import step.core.metrics.MetricTypeRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.core.timeseries.metric.MetricAggregation;
import step.core.timeseries.metric.MetricAggregationType;
import step.core.timeseries.metric.MetricRenderingSettings;
import step.core.timeseries.metric.MetricType;
import step.functions.execution.ConfigurableTokenLifecycleStrategy;
import step.grid.Grid;
import step.grid.GridImpl;
import step.grid.GridImpl.GridImplConfig;
import step.grid.TokenWrapperState;
import step.grid.client.GridClient;
import step.grid.client.GridClientConfiguration;
import step.grid.client.LocalGridClientImpl;
import step.grid.client.TokenLifecycleStrategy;
import step.grid.client.reports.GridReportBuilder;
import step.grid.client.reports.TokenGroupCapacity;
import step.grid.contextbuilder.ExecutionContextCacheConfiguration;
import step.grid.filemanager.FileManagerConfiguration;
import step.grid.filemanager.FileManagerImplConfig;
import step.grid.io.AgentErrorCode;
import step.grid.security.JwtAuthenticationFilter;
import step.grid.security.SymmetricSecurityConfiguration;
import step.resources.ResourceManagerControllerPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.core.metrics.InstrumentType.GAUGE;
import static step.core.metrics.MetricsConstants.GRID_TOKEN_AGENT_TYPE;
import static step.core.metrics.MetricsConstants.GRID_TOKEN_STATE;
import static step.core.metrics.MetricsControllerPlugin.IS_CONTROLLER_METRIC;
import static step.grid.agent.AgentTypes.AGENT_TYPE_KEY;

@Plugin(dependencies = {ResourceManagerControllerPlugin.class})
public class GridPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(GridPlugin.class);
    public static final String GRID_FILEMANAGER_CACHE_MAXIMUMSIZE = "grid.filemanager.cache.maximumsize";
    public static final String GRID_FILEMANAGER_CACHE_EXPIREAFTER_MS = "grid.filemanager.cache.expireafter.ms";
    public static final String GRID_FILENAMANGER_FILE_CLEANUP_ENABLED = "grid.filenamanger.file.cleanup.enabled";
    public static final String GRID_FILENAMANGER_FILE_CLEANUP_INTERVAL_MINUTES = "grid.filenamanger.file.cleanup.interval.minutes";
    public static final String GRID_FILEMANAGER_FILE_CLEANUP_LAST_ACCESS_THRESHOLD_MINUTES = "grid.filemanager.file.cleanup.last.access.threshold.minutes";
    public static final String GRID_SECURITY_JWT_SECRET_KEY = "grid.security.jwtSecretKey";

    public static String GRID_SAMPLER_NAME = "grid_tokens_sampler";
    public static String GRID_BY_STATE_METRIC_NAME = "grid_tokens_by_state";
    public static String GRID_CAPACITY_METRIC_NAME = "grid_tokens_capacity";

    private final Map<String,String> agentTypeMapping = Map.of("default", "Java", "node", "Node.js", "dotnet", ".NET");

    private GridImpl grid;
    private GridClient client;
    private static final String GRID_FILEMANAGER_CACHE_CONCURRENCYLEVEL_KEY = "grid.filemanager.cache.concurrencylevel";

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        Configuration configuration = context.getConfiguration();

        Integer gridPort = configuration.getPropertyAsInteger("grid.port", 8081);
        Integer tokenTTL = configuration.getPropertyAsInteger("grid.ttl", 60000);
        String jwtSecretKey = configuration.getProperty(GRID_SECURITY_JWT_SECRET_KEY);

        String fileManagerPath = configuration.getProperty("grid.filemanager.path", "filemanager");

        GridImplConfig gridConfig = new GridImplConfig();
        gridConfig.setTtl(tokenTTL);
        SymmetricSecurityConfiguration gridSecurity = new SymmetricSecurityConfiguration(jwtSecretKey);
        gridConfig.setSecurity(gridSecurity);
        if (gridSecurity.isJwtAuthenticationEnabled()) {
            context.getServiceRegistrationCallback().register(new JwtAuthenticationFilter(gridSecurity.jwtSecretKey));
        }

        gridConfig.setTokenAffinityEvaluatorClass(configuration.getProperty("grid.tokens.affinityevaluator.classname"));
        Map<String, String> tokenAffinityEvaluatorProperties = configuration.getPropertyNames().stream().filter(p -> (p instanceof String && p.toString().startsWith("grid.tokens.affinityevaluator")))
            .collect(Collectors.toMap(p -> p.toString().replace("grid.tokens.affinityevaluator.", ""), p -> configuration.getProperty(p.toString())));
        gridConfig.setTokenAffinityEvaluatorProperties(tokenAffinityEvaluatorProperties);

        FileManagerImplConfig fileManagerConfig = new FileManagerImplConfig();
        fileManagerConfig.setFileLastModificationCacheConcurrencyLevel(configuration.getPropertyAsInteger(GRID_FILEMANAGER_CACHE_CONCURRENCYLEVEL_KEY, 4));
        fileManagerConfig.setFileLastModificationCacheMaximumsize(configuration.getPropertyAsInteger(GRID_FILEMANAGER_CACHE_MAXIMUMSIZE, 1000));
        fileManagerConfig.setFileLastModificationCacheExpireAfter(configuration.getPropertyAsInteger(GRID_FILEMANAGER_CACHE_EXPIREAFTER_MS, 500));
        fileManagerConfig.setEnableCleanup(configuration.getPropertyAsBoolean(GRID_FILENAMANGER_FILE_CLEANUP_ENABLED, true));
        fileManagerConfig.setCleanupFrequencyMinutes(configuration.getPropertyAsInteger(GRID_FILENAMANGER_FILE_CLEANUP_INTERVAL_MINUTES, 60));
        fileManagerConfig.setCleanupTimeToLiveMinutes(configuration.getPropertyAsLong(GRID_FILEMANAGER_FILE_CLEANUP_LAST_ACCESS_THRESHOLD_MINUTES, 1440L));
        gridConfig.setFileManagerImplConfig(fileManagerConfig);

        GridConfigurationEditor.List editors = context.get(GridConfigurationEditor.List.class);
        if (editors != null) {
            editors.forEach(m -> m.editConfiguration(gridConfig));
        }

        grid = new GridImpl(new File(fileManagerPath), gridPort, gridConfig);
        try {
            grid.start();
        } catch (Throwable e) {
            try {
                grid.stop();
            } catch (Throwable t) {
                //ignore
            }
            throw new PluginCriticalException("An exception occurred when trying to start the Grid plugin: " + e.getClass().getName() + ": " + e.getMessage());
        }

        TokenLifecycleStrategy tokenLifecycleStrategy = getTokenLifecycleStrategy(configuration);

        GridClientConfiguration gridClientConfiguration = buildGridClientConfiguration(configuration, fileManagerConfig, gridSecurity);
        client = new LocalGridClientImpl(gridClientConfiguration, tokenLifecycleStrategy, grid);

        context.put(TokenLifecycleStrategy.class, tokenLifecycleStrategy);
        context.put(Grid.class, grid);
        context.put(GridImpl.class, grid);
        context.put(GridClient.class, client);

        context.getServiceRegistrationCallback().registerService(GridServices.class);

        configureGridMonitoring(client, context.require(MetricTypeRegistry.class));
    }

    protected ConfigurableTokenLifecycleStrategy getTokenLifecycleStrategy(Configuration configuration) {
        String AgentErrorCodeProperty = configuration.getProperty("grid.client.token.lifecycle.remove.on.agenterrors", AgentErrorCode.TIMEOUT_REQUEST_NOT_INTERRUPTED.toString());
        Set<AgentErrorCode> agentErrors;
        if (AgentErrorCodeProperty.equals("")) {
            agentErrors = Stream.of(AgentErrorCode.values()).collect(Collectors.toSet());
        } else {
            agentErrors = Arrays.asList(AgentErrorCodeProperty.split(",")).stream().map(v -> AgentErrorCode.valueOf(v)).collect(Collectors.toSet());
        }

        return new ConfigurableTokenLifecycleStrategy(
            configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokenreleaseerror", true),
            configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokenreservationerror", true),
            configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokencallerror", true),
            configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.agenterror", true),
            agentErrors);
    }

    protected GridClientConfiguration buildGridClientConfiguration(Configuration configuration, FileManagerConfiguration fileManagerConfig, SymmetricSecurityConfiguration gridSecurity) {
        GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
        gridClientConfiguration.setNoMatchExistsTimeout(configuration.getPropertyAsLong("grid.client.token.selection.nomatch.timeout.ms", gridClientConfiguration.getNoMatchExistsTimeout()));
        gridClientConfiguration.setMatchExistsTimeout(configuration.getPropertyAsLong("grid.client.token.selection.matchexist.timeout.ms", gridClientConfiguration.getMatchExistsTimeout()));
        gridClientConfiguration.setMaxConnectionRetries(configuration.getPropertyAsInteger("grid.client.token.connection.retries.max", gridClientConfiguration.getMaxConnectionRetries()));
        gridClientConfiguration.setConnectionRetryGracePeriod(configuration.getPropertyAsLong("grid.client.token.connection.retries.graceperiod", gridClientConfiguration.getConnectionRetryGracePeriod()));
        gridClientConfiguration.setReadTimeoutOffset(configuration.getPropertyAsInteger("grid.client.token.call.readtimeout.offset.ms", gridClientConfiguration.getReadTimeoutOffset()));
        gridClientConfiguration.setReserveSessionTimeout(configuration.getPropertyAsInteger("grid.client.token.reserve.timeout.ms", gridClientConfiguration.getReserveSessionTimeout()));
        gridClientConfiguration.setReleaseSessionTimeout(configuration.getPropertyAsInteger("grid.client.token.release.timeout.ms", gridClientConfiguration.getReleaseSessionTimeout()));
        gridClientConfiguration.setAllowInvalidSslCertificates(configuration.getPropertyAsBoolean("grid.client.ssl.allowinvalidcertificate", false));
        gridClientConfiguration.setMaxStringLength(configuration.getPropertyAsInteger("grid.client.max.string.length.bytes", gridClientConfiguration.getMaxStringLength()));
        gridClientConfiguration.setLocalTokenExecutionContextCacheConfiguration(new ExecutionContextCacheConfiguration());
        gridClientConfiguration.setGridSecurity(gridSecurity);
        return gridClientConfiguration;
    }

    /**
     * Start the grid monitoring and register grid metric types
     * @param client the grid client to be monitored
     * @param metricTypeRegistry the registry used to register the grid metric type
     */
    private void configureGridMonitoring(GridClient client, MetricTypeRegistry metricTypeRegistry) {
        MetricSamplerRegistry metricSamplerRegistry = MetricSamplerRegistry.getInstance();
        metricSamplerRegistry.registerSampler(GRID_SAMPLER_NAME, new MetricSampler(GRID_SAMPLER_NAME,
            "step grid token usage and capacity") {
            final GridReportBuilder gridReportBuilder = new GridReportBuilder(client);
            @Override
            public List<ControllerMetricSample> collectMetricSamples() {
                Set<String> tokenAttributeKeys = new HashSet<>(gridReportBuilder.getTokenAttributeKeys());
                tokenAttributeKeys.removeAll(List.of("$agentid", "$tokenid"));
                List<TokenGroupCapacity> usageByIdentity = gridReportBuilder.getUsageByIdentity(tokenAttributeKeys);
                List<ControllerMetricSample> gridMetricSamples = new ArrayList<>();
                long now = System.currentTimeMillis();
                for (TokenGroupCapacity tokenGroupCapacity : usageByIdentity) {
                    int capacity = tokenGroupCapacity.getCapacity();
                    Map<String, String> labels = new TreeMap<>(tokenGroupCapacity.getKey());
                    if (labels.containsKey(AGENT_TYPE_KEY)) {
                        String value = labels.remove(AGENT_TYPE_KEY);
                        labels.put(GRID_TOKEN_AGENT_TYPE.getName(), agentTypeMapping.getOrDefault(value, value));
                    }
                    gridMetricSamples.add(new ControllerMetricSample(
                        new MetricSample(now, "grid_tokens_capacity", labels, GAUGE,
                            1, capacity, capacity, capacity, capacity, null),
                        GRID_CAPACITY_METRIC_NAME));
                    for (TokenWrapperState state : TokenWrapperState.values()) {
                        int valueByState = Objects.requireNonNullElse(tokenGroupCapacity.getCountByState().get(state), 0);
                        TreeMap<String, String> labelsWithState = new TreeMap<>(labels);
                        labelsWithState.put(GRID_TOKEN_STATE.getName(), state.name());
                        gridMetricSamples.add(new ControllerMetricSample(
                            new MetricSample(now, "grid_token_" + state.name(), labelsWithState, GAUGE,
                                1, valueByState, valueByState, valueByState, valueByState, null),
                            GRID_BY_STATE_METRIC_NAME));
                    }
                }
                return gridMetricSamples;
            }
        });

        MetricType gridTokensByState = new MetricType()
            .setName(GRID_BY_STATE_METRIC_NAME)
            .setDisplayName("Grid tokens by state")
            .setDescription("Number of grid tokens currently in each lifecycle state, broken down by state and agent type.")
            .setInstrumentType(GAUGE.toLowerCase())
            .setAttributes(Arrays.asList(GRID_TOKEN_STATE, GRID_TOKEN_AGENT_TYPE))
            .setDefaultGroupingAttributes(List.of(GRID_TOKEN_STATE.getName(), GRID_TOKEN_AGENT_TYPE.getName()))
            .setUnit("1")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
            .setRenderingSettings(new MetricRenderingSettings());
        gridTokensByState.addCustomField(IS_CONTROLLER_METRIC, true);
        metricTypeRegistry.registerMetricType(gridTokensByState);

        MetricType gridTokensCapacity = new MetricType()
            .setName(GRID_CAPACITY_METRIC_NAME)
            .setDisplayName("Grid tokens capacity")
            .setDescription("Total number of available grid tokens per agent type, representing the maximum execution concurrency of the grid.")
            .setInstrumentType(GAUGE.toLowerCase())
            .setAttributes(List.of(GRID_TOKEN_AGENT_TYPE))
            .setDefaultGroupingAttributes(List.of(GRID_TOKEN_AGENT_TYPE.getName()))
            .setUnit("1")
            .setDefaultAggregation(new MetricAggregation(MetricAggregationType.SUM))
            .setRenderingSettings(new MetricRenderingSettings());
        gridTokensCapacity.addCustomField(IS_CONTROLLER_METRIC, true);
        metricTypeRegistry.registerMetricType(gridTokensCapacity);
    }

    @Override
    public void serverStop(GlobalContext context) {
        if (client != null) {
            client.close();
        }
        if (grid != null) {
            try {
                grid.stop();
            } catch (Exception e) {
                logger.error("Error while stopping the grid server", e);
            }
        }
    }
}
