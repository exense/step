package step.ide;

import ch.exense.commons.app.Configuration;
import step.framework.server.ControllerServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public class LocalIDE implements Closeable {
    private final ControllerServer server;

    public static void main(String[] args) throws Exception {
        try {
            // TODO: make this throw an exception or at least return a success status
            new LocalIDE().start();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public LocalIDE() throws Exception {
        Configuration configuration = loadConfiguration();
        var resourcesDirectory = Files.createTempDirectory("step-ide-resources-").toFile();
        var fileManagerDirectory = Files.createTempDirectory("step-ide-filemanager-").toFile();
        LocalIDEState.get().addDirectoriesToCleanupOnShutdown(List.of(resourcesDirectory, fileManagerDirectory));
        configuration.putProperty("resources.dir", resourcesDirectory.getAbsolutePath());
        configuration.putProperty("grid.filemanager.path", fileManagerDirectory.getAbsolutePath());
        String jmeterHome = System.getenv("JMETER_HOME");
        if (jmeterHome != null) {
            configuration.putProperty("plugins.jmeter.home", jmeterHome);
        }
        server = new ControllerServer(configuration);
    }

    private static Configuration loadConfiguration() throws Exception {
        Configuration configuration = new Configuration();
        InputStream propsStream = Objects.requireNonNull(LocalIDE.class.getClassLoader().getResourceAsStream("ide.properties"), "ide.properties resource not found");
        configuration.getUnderlyingPropertyObject().load(propsStream);
        return configuration;
    }

    private void start() throws Exception {
        server.start();
    }

    @Override
    public void close() throws IOException {
        System.err.println("TODO SED-4429: shutdown server");
    }
}
