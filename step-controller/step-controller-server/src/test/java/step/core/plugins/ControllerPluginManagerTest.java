package step.core.plugins;

import ch.exense.commons.app.Configuration;
import org.junit.Test;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.ServerPluginManager;

import java.util.List;

import static org.junit.Assert.*;

public class ControllerPluginManagerTest {

    public static final WebPlugin WEB_PLUGIN = new WebPlugin();
    public static final AbstractExecutionEnginePlugin EXECUTION_ENGINE_PLUGIN = new AbstractExecutionEnginePlugin() {
    };

    @Test
    public void test() throws Exception {
        ControllerPluginManager pluginManager = new ControllerPluginManager(new ServerPluginManager(new Configuration()));
        List<AbstractWebPlugin> plugins = pluginManager.getWebPlugins();
        assertTrue(plugins.contains(WEB_PLUGIN));
        List<ExecutionEnginePlugin> executionEnginePlugins = pluginManager.getExecutionEnginePlugins();
        assertTrue(executionEnginePlugins.contains(EXECUTION_ENGINE_PLUGIN));
    }

    @Test
    public void testDisabling() throws Exception {
        Configuration configuration = new Configuration();
        configuration.putProperty("plugins.TestPlugin.enabled", "false");
        assertThrows(PluginCriticalException.class, () -> new ControllerPluginManager(new ServerPluginManager(configuration)));
    }

    @Plugin
    public static class TestPlugin extends AbstractControllerPlugin {

        @Override
        public boolean canBeDisabled() {
            return false;
        }

        @Override
        public AbstractWebPlugin getWebPlugin() {
            return WEB_PLUGIN;
        }

        @Override
        public ExecutionEnginePlugin getExecutionEnginePlugin() {
            return EXECUTION_ENGINE_PLUGIN;
        }
    }
}