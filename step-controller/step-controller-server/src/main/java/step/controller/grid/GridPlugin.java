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

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.controller.grid.services.GridServices;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.functions.execution.ConfigurableTokenLifecycleStrategy;
import step.grid.Grid;
import step.grid.GridImpl;
import step.grid.GridImpl.GridImplConfig;
import step.grid.client.GridClient;
import step.grid.client.GridClientConfiguration;
import step.grid.client.LocalGridClientImpl;
import step.grid.client.TokenLifecycleStrategy;
import step.grid.filemanager.FileManagerImplConfig;
import step.grid.io.AgentErrorCode;
import step.resources.ResourceManagerControllerPlugin;

@Plugin(dependencies= {ResourceManagerControllerPlugin.class})
public class GridPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(GridPlugin.class);
	public static final String GRID_FILEMANAGER_CACHE_MAXIMUMSIZE = "grid.filemanager.cache.maximumsize";
	public static final String GRID_FILEMANAGER_CACHE_EXPIREAFTER_MS = "grid.filemanager.cache.expireafter.ms";
	public static final String GRID_FILENAMANGER_FILE_CLEANUP_ENABLED = "grid.filenamanger.file.cleanup.enabled";
	public static final String GRID_FILENAMANGER_FILE_CLEANUP_INTERVAL_MINUTES = "grid.filenamanger.file.cleanup.interval.minutes";
	public static final String GRID_FILEMANAGER_FILE_CLEANUP_LAST_ACCESS_THRESHOLD_MINUTES = "grid.filemanager.file.cleanup.last.access.threshold.minutes";

	private GridImpl grid;
	private GridClient client;
	private static final String GRID_FILEMANAGER_CACHE_CONCURRENCYLEVEL_KEY = "grid.filemanager.cache.concurrencylevel";

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		Configuration configuration = context.getConfiguration();
		
		Integer gridPort = configuration.getPropertyAsInteger("grid.port",8081);
		Integer tokenTTL = configuration.getPropertyAsInteger("grid.ttl",60000);
		
		String fileManagerPath = configuration.getProperty("grid.filemanager.path", "filemanager");
		
		GridImplConfig gridConfig = new GridImplConfig();
		gridConfig.setTtl(tokenTTL);

		gridConfig.setTokenAffinityEvaluatorClass(configuration.getProperty("grid.tokens.affinityevaluator.classname"));
		Map<String, String> tokenAffinityEvaluatorProperties = configuration.getPropertyNames().stream().filter(p->(p instanceof String && p.toString().startsWith("grid.tokens.affinityevaluator")))
			.collect(Collectors.toMap(p->p.toString().replace("grid.tokens.affinityevaluator.", ""), p->configuration.getProperty(p.toString())));
		gridConfig.setTokenAffinityEvaluatorProperties(tokenAffinityEvaluatorProperties);

		FileManagerImplConfig fileManagerConfig = new FileManagerImplConfig();
		fileManagerConfig.setFileLastModificationCacheConcurrencyLevel(configuration.getPropertyAsInteger(GRID_FILEMANAGER_CACHE_CONCURRENCYLEVEL_KEY, 4));
		fileManagerConfig.setFileLastModificationCacheMaximumsize(configuration.getPropertyAsInteger(GRID_FILEMANAGER_CACHE_MAXIMUMSIZE, 1000));
		fileManagerConfig.setFileLastModificationCacheExpireAfter(configuration.getPropertyAsInteger(GRID_FILEMANAGER_CACHE_EXPIREAFTER_MS, 500));
		fileManagerConfig.setCleanupJobEnabled(configuration.getPropertyAsBoolean(GRID_FILENAMANGER_FILE_CLEANUP_ENABLED, true));
		fileManagerConfig.setCleanupIntervalMinutes(configuration.getPropertyAsInteger(GRID_FILENAMANGER_FILE_CLEANUP_INTERVAL_MINUTES, 60));
		fileManagerConfig.setCleanupLastAccessTimeThresholdMinutes(configuration.getPropertyAsLong(GRID_FILEMANAGER_FILE_CLEANUP_LAST_ACCESS_THRESHOLD_MINUTES, 120L));
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
		
		GridClientConfiguration gridClientConfiguration = buildGridClientConfiguration(configuration);
		client = new LocalGridClientImpl(gridClientConfiguration, tokenLifecycleStrategy, grid);

		context.put(TokenLifecycleStrategy.class, tokenLifecycleStrategy);
		context.put(Grid.class, grid);
		context.put(GridImpl.class, grid);
		context.put(GridClient.class, client);
		
		context.getServiceRegistrationCallback().registerService(GridServices.class);
	}

	protected ConfigurableTokenLifecycleStrategy getTokenLifecycleStrategy(Configuration configuration) {
		String AgentErrorCodeProperty = configuration.getProperty("grid.client.token.lifecycle.remove.on.agenterrors", AgentErrorCode.TIMEOUT_REQUEST_NOT_INTERRUPTED.toString());
		Set<AgentErrorCode> agentErrors;
		if (AgentErrorCodeProperty.equals("")) {
			agentErrors = Stream.of(AgentErrorCode.values()).collect(Collectors.toSet());
		} else {
			agentErrors = Arrays.asList(AgentErrorCodeProperty.split(",")).stream().map(v->AgentErrorCode.valueOf(v)).collect(Collectors.toSet());
		}

		return new ConfigurableTokenLifecycleStrategy(
				configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokenreleaseerror", true),
				configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokenreservationerror", true),
				configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.tokencallerror", true),
				configuration.getPropertyAsBoolean("grid.client.token.lifecycle.remove.on.agenterror", true),
				agentErrors);
	}

	protected GridClientConfiguration buildGridClientConfiguration(Configuration configuration) {
		GridClientConfiguration gridClientConfiguration = new GridClientConfiguration();
		gridClientConfiguration.setNoMatchExistsTimeout(configuration.getPropertyAsLong("grid.client.token.selection.nomatch.timeout.ms", gridClientConfiguration.getNoMatchExistsTimeout()));
		gridClientConfiguration.setMatchExistsTimeout(configuration.getPropertyAsLong("grid.client.token.selection.matchexist.timeout.ms", gridClientConfiguration.getMatchExistsTimeout()));
		gridClientConfiguration.setReadTimeoutOffset(configuration.getPropertyAsInteger("grid.client.token.call.readtimeout.offset.ms", gridClientConfiguration.getReadTimeoutOffset()));
		gridClientConfiguration.setReserveSessionTimeout(configuration.getPropertyAsInteger("grid.client.token.reserve.timeout.ms", gridClientConfiguration.getReserveSessionTimeout()));
		gridClientConfiguration.setReleaseSessionTimeout(configuration.getPropertyAsInteger("grid.client.token.release.timeout.ms", gridClientConfiguration.getReleaseSessionTimeout()));
		gridClientConfiguration.setAllowInvalidSslCertificates(configuration.getPropertyAsBoolean("grid.client.ssl.allowinvalidcertificate", false));
		gridClientConfiguration.setMaxStringLength(configuration.getPropertyAsInteger("grid.client.max.string.length.bytes", gridClientConfiguration.getMaxStringLength()));
		return gridClientConfiguration;
	}

	@Override
	public void serverStop(GlobalContext context) {
		if(client!=null) {
			client.close();
		}
		if(grid!=null) {
			try {
				grid.stop();
			} catch (Exception e) {
				logger.error("Error while stopping the grid server",e);
			}
		}
	}
}
