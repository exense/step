package step.ide;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.scheduler.ExecutionScheduler;
import step.framework.server.ControllerServer;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class LocalIDE {

    private static final Logger logger = LoggerFactory.getLogger(LocalIDE.class);
    private static final String OVERLAY_FILE_NAME = "ide.properties";
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
        var model = LocalIDEModel.get();
        Configuration configuration = loadConfiguration();
        for (var cfg : model.startupHooks.onConfigure) {
            // Startup hooks could throw exceptions, or outright stop the entire execution using System.exit.
            // That's intentional, and the reason why they're called as early as possible.
            cfg.accept(configuration);
        }
        var fileManagerDirectory = Files.createTempDirectory("step-ide-filemanager-");
        String configuredResourcesDirectory = configuration.getProperty("resources.dir");
        if (configuredResourcesDirectory == null || configuredResourcesDirectory.isBlank()) {
            // No resources directory configured: the resources only live as long as the IDE
            var resourcesDirectory = Files.createTempDirectory("step-ide-resources-");
            model.addDirectoriesToCleanupOnShutdown(List.of(resourcesDirectory, fileManagerDirectory));
            configuration.putProperty("resources.dir", resourcesDirectory.toString());
        } else {
            logger.info("Using the configured resources directory: {}", new File(configuredResourcesDirectory).getAbsolutePath());
            model.addDirectoriesToCleanupOnShutdown(List.of(fileManagerDirectory));
        }
        configuration.putProperty("grid.filemanager.path", fileManagerDirectory.toString());
        configuration.putProperty("ui.resource.root", model.getIdeResourcePath());
        // The schedules of the opened automation package are edited in the IDE, never triggered by it
        configuration.putProperty(ExecutionScheduler.CONFIGURATION_SCHEDULING_ALLOWED, Boolean.FALSE.toString());
        server = new IDEControllerServer(configuration);
        model.setPort(server.getPort());
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
        try (InputStream propsStream = Objects.requireNonNull(LocalIDE.class.getClassLoader().getResourceAsStream("ide.properties"), "ide.properties resource not found");) {
            configuration.getUnderlyingPropertyObject().load(propsStream);
        }
        // Overlay an external ide.properties if present, so that users can configure the IDE (e.g. the AI agent
        // package location or an API key) without modifying the packaged resource.
        Path externalProperties = findOverlay();
        if (externalProperties != null) {
            try (InputStream externalStream = Files.newInputStream(externalProperties)) {
                configuration.getUnderlyingPropertyObject().load(externalStream);
            }
        }
        return configuration;
    }

    /**
     * @return the overlay file to apply: the one designated by the system property {@value #OVERLAY_FILE_NAME} if
     * set, otherwise the one of the working directory, otherwise the one next to the executable. Null if there is none.
     */
    private static Path findOverlay() {
        String explicitLocation = System.getProperty(OVERLAY_FILE_NAME);
        if (explicitLocation != null && !explicitLocation.isBlank()) {
            Path explicitOverlay = Path.of(explicitLocation).toAbsolutePath().normalize();
            logger.info("Looking for overlay {} at {} (system property {})", OVERLAY_FILE_NAME, explicitOverlay, OVERLAY_FILE_NAME);
            return applicableOverlay(explicitOverlay, true);
        }
        Path workingDirectoryOverlay = Path.of(OVERLAY_FILE_NAME).toAbsolutePath().normalize();
        Path installationDirectory = getInstallationDirectory();
        Path installationOverlay = installationDirectory == null ? null : installationDirectory.resolve(OVERLAY_FILE_NAME);
        if (installationOverlay == null || installationOverlay.equals(workingDirectoryOverlay)) {
            logger.info("Looking for overlay {} in {}", OVERLAY_FILE_NAME, workingDirectoryOverlay.getParent());
            return applicableOverlay(workingDirectoryOverlay);
        }
        logger.info("Looking for overlay {} in {} (working directory) and {} (installation directory)", OVERLAY_FILE_NAME, workingDirectoryOverlay.getParent(), installationDirectory);
        if (Files.isRegularFile(workingDirectoryOverlay)) {
            if (Files.isRegularFile(installationOverlay)) {
                logger.info("Overlay {} found in both locations. Applying the one of the working directory: {}. Ignoring: {}", OVERLAY_FILE_NAME, workingDirectoryOverlay, installationOverlay);
                return workingDirectoryOverlay;
            }
            return applicableOverlay(workingDirectoryOverlay);
        }
        return applicableOverlay(installationOverlay);
    }

    private static Path applicableOverlay(Path overlay) {
        return applicableOverlay(overlay, false);
    }

    private static Path applicableOverlay(Path overlay, boolean warn) {
        if (Files.isRegularFile(overlay)) {
            logger.info("Applying overlay configuration file: {}", overlay);
            return overlay;
        } else if (warn) {
            logger.warn("No overlay configuration file found under: {}", overlay);
        }
        return null;
    }

    /**
     * @return the directory containing the executable (or jar) the IDE runs from, independently of the working
     * directory and of the shell it was started from. Null if it cannot be determined.
     */
    private static Path getInstallationDirectory() {
        // Started from the executable (step.exe, the step script or the jar), the class path is that single file. The
        // classes of the IDE are then in a jar nested in it, whose location is not a file of the file system
        String classPath = System.getProperty("java.class.path", "");
        if (!classPath.isBlank() && !classPath.contains(File.pathSeparator)) {
            Path executable = Path.of(classPath).toAbsolutePath().normalize();
            if (Files.isRegularFile(executable)) {
                return executable.getParent();
            }
        }
        try {
            URL codeLocationUrl = LocalIDE.class.getProtectionDomain().getCodeSource().getLocation();
            if (!"file".equals(codeLocationUrl.getProtocol())) {
                logger.warn("Unable to determine the installation directory of the IDE from {}, the overlay {} is only looked up in the working directory", codeLocationUrl, OVERLAY_FILE_NAME);
                return null;
            }
            Path codeLocation = Path.of(codeLocationUrl.toURI()).toAbsolutePath().normalize();
            return Files.isDirectory(codeLocation) ? codeLocation : codeLocation.getParent();
        } catch (Exception e) {
            logger.warn("Unable to determine the installation directory of the IDE, the overlay {} is only looked up in the working directory", OVERLAY_FILE_NAME, e);
            return null;
        }
    }

    public void start() throws Exception {
        server.start();
    }
}
