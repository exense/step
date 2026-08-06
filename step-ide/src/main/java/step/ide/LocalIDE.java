package step.ide;

import ch.exense.commons.app.Configuration;
import step.framework.server.ControllerServer;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public class LocalIDE {
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
        String jmeterHome = System.getenv("JMETER_HOME");
        if (jmeterHome != null) {
            configuration.putProperty("plugins.jmeter.home", jmeterHome);
        }
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

    private static Configuration loadConfiguration() throws Exception {
        Configuration configuration = new Configuration();
        InputStream propsStream = Objects.requireNonNull(LocalIDE.class.getClassLoader().getResourceAsStream("ide.properties"), "ide.properties resource not found");
        configuration.getUnderlyingPropertyObject().load(propsStream);
        return configuration;
    }

    public void start() throws Exception {
        server.start();
    }
}
