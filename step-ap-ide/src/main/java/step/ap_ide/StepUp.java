package step.ap_ide;

import ch.exense.commons.app.Configuration;
import org.apache.commons.io.FileUtils;
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

    private static final String workDirName = "work";
    private static final String initialDirName = "src/main/resources/work-initial";

    public static void main(String[] args) throws Exception {
        ControllerServer.main(args);
        init();
        App.main(args); // this will never return
    }

    private static void init() throws Exception {
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
        Properties properties = new Properties();
        setPropertiesWriteToFragment(properties, YamlPlan.PLANS_ENTITY_NAME, "plans.yml");
        setPropertiesWriteToFragment(properties, Parameter.ENTITY_NAME, "parameters4.yml");
        fragmentManager.setProperties(properties);
        var automationPackageCollectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);

        // this will allow to switch between APs
        CurrentlyOpenedAutomationPackageCollectionFactory.getInstance().setCurrentFactory(automationPackageCollectionFactory);
    }

    private static void setPropertiesWriteToFragment(Properties properties, String entityName, String fragment) {
        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, entityName), fragment);
        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, entityName), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());
    }
}
