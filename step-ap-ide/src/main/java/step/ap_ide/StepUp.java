package step.ap_ide;

import ch.exense.commons.app.Configuration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.JavaAutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.collections.AutomationPackageCollectionFactory;
import step.framework.server.ControllerServer;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.util.Objects;
import java.util.Properties;

public class StepUp {

    private static final Logger logger = LoggerFactory.getLogger(StepUp.class);

    private static final String workDirName = "work";
    private static final String initialDirName = "src/main/resources/work-initial";

    public static void main(String[] args) throws Exception {
        // Use an IntelliJ run configuration that uses '%MODULE_WORKING_DIR%' (verbatim) as the working directory, and use
        // -config="src/test/resources/step.properties" as program argument
        ControllerServer.main(args);
        initWorkdir();
        App.main(args); // this will never return
    }

    static final JavaAutomationPackageReader READER;

    static {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();

        // required for reading parameters, apparently the manager can be null
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, null);

        READER = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
    }

    private static void initWorkdir() throws Exception {
        File workDir = new File(workDirName);
        if (!workDir.isDirectory() || (workDir.isDirectory() && Objects.requireNonNull(workDir.listFiles()).length == 0)) {
            logger.info("Work directory is not present or empty, initializing from " + initialDirName);
            File initialDir = new File(initialDirName);
            if (!initialDir.isDirectory()) {
                throw new RuntimeException("Not a directory: " + initialDir.getAbsolutePath());
            }
            FileUtils.copyDirectory(initialDir, workDir);
            if (!workDir.isDirectory()) {
                throw new RuntimeException("Something went wrong while initializing directory: " + workDir.getAbsolutePath());
            }
        } else {
            logger.info("Using existing work directory at: " + workDir.getAbsolutePath());
            logger.info("You may delete this directory to start over from scratch");
        }
        useAutomationPackageDirectory(workDir);
    }

//    private static void setPropertiesWriteToFragment(Properties properties, String entityName, String fragment) {
//        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, entityName), fragment);
//        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, entityName), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());
//    }

    static void useAutomationPackageDirectory(File apDir) throws Exception {
        var fragmentManager = StepUp.READER.getAutomationPackageYamlFragmentManager(apDir);
        Properties properties = new Properties();
        // parameters all go into parameters.yml
        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, Parameter.ENTITY_NAME), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());
        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, Parameter.ENTITY_NAME), "parameters.yml");
        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, YamlPlan.PLANS_ENTITY_NAME), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.PER_OBJECT.name());
        fragmentManager.setProperties(properties);
        var automationPackageCollectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);

        CurrentlyOpenedAutomationPackageCollectionFactory.getInstance().setCurrentFactory(automationPackageCollectionFactory);
    }
}
