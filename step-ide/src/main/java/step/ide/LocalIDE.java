package step.ide;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.framework.server.ControllerServer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

public class LocalIDE implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(LocalIDE.class);
    private final ControllerServer server;
    private final File resourcesDirectory;
    private final File fileManagerDirectory;

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
        resourcesDirectory = Files.createTempDirectory("step-ide-resources-").toFile();
        fileManagerDirectory = Files.createTempDirectory("step-ide-filemanager-").toFile();
        // TODO: delete on exit
        logger.info("Using temporary resources directory: {}", resourcesDirectory.getAbsolutePath());
        configuration.putProperty("resources.dir", resourcesDirectory.getAbsolutePath());
        logger.info("Using temporary filemanager directory: {}", fileManagerDirectory.getAbsolutePath());
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
        System.err.println("TODO: shutdown server");
    }
}
