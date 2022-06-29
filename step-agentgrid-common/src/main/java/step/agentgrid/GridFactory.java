package step.agentgrid;

import ch.exense.commons.app.Configuration;
import step.core.plugins.exceptions.PluginCriticalException;
import step.grid.GridImpl;
import step.grid.filemanager.FileManager;
import step.grid.filemanager.FileManagerImpl;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

public class GridFactory {

    public GridImpl initGrid(Configuration configuration) {
        GridImpl.GridImplConfig gridConfig = getGridConfig(configuration);
        Integer gridPort = configuration.getPropertyAsInteger("grid.port", 8081);

        String fileManagerPath = configuration.getProperty("grid.filemanager.path", "filemanager");

        GridImpl grid = new GridImpl(new File(fileManagerPath), gridPort, gridConfig);
        try {
            grid.start();
            return grid;
        } catch (Throwable e) {
            try {
                grid.stop();
            } catch (Throwable t) {
                //ignore
            }
            throw new PluginCriticalException("An exception occurred when trying to start the Grid plugin: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public FileManager initFileManager(Configuration configuration) {
        String fileManagerPath = configuration.getProperty("grid.filemanager.path", "filemanager");

        GridImpl.GridImplConfig gridConfig = getGridConfig(configuration);
        FileManagerImpl.FileManagerImplConfig config = new FileManagerImpl.FileManagerImplConfig();
        config.setFileLastModificationCacheConcurrencyLevel(gridConfig.getFileLastModificationCacheConcurrencyLevel());
        config.setFileLastModificationCacheExpireAfter(gridConfig.getFileLastModificationCacheExpireAfter());
        config.setFileLastModificationCacheMaximumsize(gridConfig.getFileLastModificationCacheMaximumsize());
        return new FileManagerImpl(new File(fileManagerPath), config);
    }

    private GridImpl.GridImplConfig getGridConfig(Configuration configuration) {
        GridImpl.GridImplConfig gridConfig = new GridImpl.GridImplConfig();
        Integer tokenTTL = configuration.getPropertyAsInteger("grid.ttl", 60000);
        gridConfig.setFileLastModificationCacheConcurrencyLevel(configuration.getPropertyAsInteger("grid.filemanager.cache.concurrencylevel", 4));
        gridConfig.setFileLastModificationCacheMaximumsize(configuration.getPropertyAsInteger("grid.filemanager.cache.maximumsize", 1000));
        gridConfig.setFileLastModificationCacheExpireAfter(configuration.getPropertyAsInteger("grid.filemanager.cache.expireafter.ms", 500));
        gridConfig.setTtl(tokenTTL);

        gridConfig.setTokenAffinityEvaluatorClass(configuration.getProperty("grid.tokens.affinityevaluator.classname"));
        Map<String, String> tokenAffinityEvaluatorProperties = configuration.getPropertyNames().stream().filter(p -> (p instanceof String && p.toString().startsWith("grid.tokens.affinityevaluator")))
                .collect(Collectors.toMap(p -> p.toString().replace("grid.tokens.affinityevaluator.", ""), p -> configuration.getProperty(p.toString())));
        gridConfig.setTokenAffinityEvaluatorProperties(tokenAffinityEvaluatorProperties);
        return gridConfig;
    }
}
