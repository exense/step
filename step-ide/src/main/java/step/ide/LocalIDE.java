package step.ide;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.framework.server.ControllerServer;
import step.ide.ai.IDEAiConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public class LocalIDE {

    private static final Logger logger = LoggerFactory.getLogger(LocalIDE.class);
    private final ControllerServer server;

    public static void main(String[] args) throws Exception {
        try {
            new LocalIDE().start();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public LocalIDE() throws Exception {
        Configuration configuration = loadConfiguration();
        var resourcesDirectory = Files.createTempDirectory("step-ide-resources-");
        var fileManagerDirectory = Files.createTempDirectory("step-ide-filemanager-");
        LocalIDEState.get().addDirectoriesToCleanupOnShutdown(List.of(resourcesDirectory, fileManagerDirectory));
        configuration.putProperty("resources.dir", resourcesDirectory.toString());
        configuration.putProperty("grid.filemanager.path", fileManagerDirectory.toString());
        configuration.putProperty("ui.resource.root", LocalIDEState.getIdeResourcePath());
        applyEnvOverride(configuration, "JMETER_HOME", "plugins.jmeter.home");
        applyEnvOverride(configuration, IDEAiConfiguration.ENV_AGENT_PACKAGE, IDEAiConfiguration.PROP_AGENT_PACKAGE);
        applyEnvOverride(configuration, IDEAiConfiguration.ENV_AGENT_PLAN, IDEAiConfiguration.PROP_AGENT_PLAN);
        applyEnvOverride(configuration, IDEKeywordPropertiesPlugin.ENV_ANTHROPIC_API_KEY,
            IDEKeywordPropertiesPlugin.KEYWORD_PROPERTY_PREFIX + IDEKeywordPropertiesPlugin.ENV_ANTHROPIC_API_KEY);
        server = new IDEControllerServer(configuration);
    }

    private static class IDEControllerServer extends ControllerServer {
        static {
            // method is protected, so we need a subclass
            setupLogging();
        }

        public IDEControllerServer(Configuration configuration) {
            super(configuration);
        }
    }

    /**
     * Applies an environment variable on top of the configuration. Environment variables take precedence over
     * ide.properties, which notably allows secrets to be provided without ending up in a file.
     */
    private static void applyEnvOverride(Configuration configuration, String environmentVariable, String propertyKey) {
        String value = System.getenv(environmentVariable);
        if (value != null && !value.isBlank()) {
            logger.info("Applying environment variable {} to property {}", environmentVariable, propertyKey);
            configuration.putProperty(propertyKey, value);
        }
    }

    private static Configuration loadConfiguration() throws Exception {
        Configuration configuration = new Configuration();
        InputStream propsStream = Objects.requireNonNull(LocalIDE.class.getClassLoader().getResourceAsStream("ide.properties"), "ide.properties resource not found");
        configuration.getUnderlyingPropertyObject().load(propsStream);
        // Overlay an external ide.properties if present, so that users can configure the IDE (e.g. the AI agent
        // package location or an API key) without modifying the packaged resource.
        File externalProperties = new File(System.getProperty("ide.properties", "ide.properties"));
        if (externalProperties.isFile()) {
            logger.info("Overlaying external configuration file: {}", externalProperties.getAbsolutePath());
            try (InputStream externalStream = Files.newInputStream(externalProperties.toPath())) {
                configuration.getUnderlyingPropertyObject().load(externalStream);
            }
        }
        return configuration;
    }

    public void start() throws Exception {
        server.start();
    }
}
