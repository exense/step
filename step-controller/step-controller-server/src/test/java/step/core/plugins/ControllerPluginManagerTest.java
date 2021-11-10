package step.core.plugins;

import ch.exense.commons.app.Configuration;
import org.junit.Test;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.BasePlugin;
import step.engine.plugins.ExecutionEnginePlugin;

import java.util.List;

import static org.junit.Assert.*;

public class ControllerPluginManagerTest {

    public static final WebPlugin WEB_PLUGIN = new WebPlugin();
    public static final AbstractExecutionEnginePlugin EXECUTION_ENGINE_PLUGIN = new AbstractExecutionEnginePlugin() {
    };

    @Test
    public void test() throws Exception {
        ControllerPluginManager pluginManager = new ControllerPluginManager(new Configuration());
        List<WebPlugin> plugins = pluginManager.getWebPlugins();
        assertTrue(plugins.contains(WEB_PLUGIN));
        List<ExecutionEnginePlugin> executionEnginePlugins = pluginManager.getExecutionEnginePlugins();
        assertTrue(executionEnginePlugins.contains(EXECUTION_ENGINE_PLUGIN));
    }

    @Test
    public void testDisabling() throws Exception {
        Configuration configuration = new Configuration();
        configuration.putProperty("plugins.TestPlugin.enabled", "false");
        assertThrows(PluginCriticalException.class, () -> new ControllerPluginManager(configuration));
    }

    @Plugin
    public static class TestPlugin extends AbstractControllerPlugin {

        @Override
        public boolean canBeDisabled() {
            return false;
        }

        @Override
        public WebPlugin getWebPlugin() {
            return WEB_PLUGIN;
        }

        @Override
        public ExecutionEnginePlugin getExecutionEnginePlugin() {
            return EXECUTION_ENGINE_PLUGIN;
        }
    }
}