package step.ap_ide;

import ch.exense.commons.app.Configuration;
import org.apache.commons.io.FileUtils;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.JavaAutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.collections.AutomationPackageCollectionFactory;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.EntityVersion;
import step.parameter.automation.AutomationPackageParametersRegistration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class MyAPCollectionFactory implements CollectionFactory {

    private static final String workDirName = "work";
    private static final String initialDirName = "src/main/resources/work-initial";
    private final AutomationPackageCollectionFactory automationPackageCollectionFactory;

    public MyAPCollectionFactory(Properties properties) {
        try {
            AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
            AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();

            // required for reading parameters, apparently the manager can be null
            AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, null);

            var reader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
            File workDir = new File(workDirName);
            if (!workDir.isDirectory() || (workDir.isDirectory() && Objects.requireNonNull(workDir.listFiles()).length == 0)) {
                System.err.println("Work directory is not present or empty, initializing from " + initialDirName);
                File initialDir = new File(initialDirName);
                if (!initialDir.isDirectory()) {
                    throw new RuntimeException("Not a directory: " + initialDir.getAbsolutePath());
                }
                FileUtils.copyDirectory(initialDir, workDir);
                if (!workDir.isDirectory()) {
                    throw new RuntimeException("Something went wrong while initializing directory: " + workDir.getAbsolutePath());
                }
            } else {
                System.err.println("Work directory is present at: " + workDir.getAbsolutePath());
                System.err.println("You may delete this directory to start over from scratch");
            }
            var fragmentManager = reader.getAutomationPackageYamlFragmentManager(workDir);
            automationPackageCollectionFactory = new AutomationPackageCollectionFactory(properties, fragmentManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Collection<T> getCollection(String name, Class<T> entityClass) {
        return automationPackageCollectionFactory.getCollection(name, entityClass);
    }

    @Override
    public Collection<EntityVersion> getVersionedCollection(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        automationPackageCollectionFactory.close();
    }
}
