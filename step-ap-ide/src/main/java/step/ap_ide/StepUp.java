package step.ap_ide;

import ch.exense.commons.app.Configuration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.JavaAutomationPackageArchive;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

public class StepUp {

    private static final Logger logger = LoggerFactory.getLogger(StepUp.class);

    // These are meant for development to have something to play with without having to recreate everything from scratch
    private static final String workDirName = "work";
    private static final String initialDirName = "src/main/resources/work-initial";

    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configuration();
        // this uses the existing methods, we could also refactor the implementation to make it a little easier.
        InputStream propsStream = StepUp.class.getClassLoader().getResourceAsStream("step.properties");
        configuration.getUnderlyingPropertyObject().load(propsStream);
        new ControllerServer(configuration).start();
        initWorkdir();
        //FXApp.main(args); // this will never return*/
    }

    private static final JavaAutomationPackageReader READER;

    static {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        // required for reading parameters, apparently the manager can be null
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, null);
        READER = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
    }

    private static void initWorkdir() throws Exception {
        File workDir = new File(workDirName);
        File initialDir = new File(initialDirName);
        if (!workDir.isDirectory() || (workDir.isDirectory() && Objects.requireNonNull(workDir.listFiles()).length == 0)) {
            logger.info("Work directory is not present or empty, initializing from " + initialDirName);
            if (!initialDir.isDirectory()) {
                workDir = Files.createTempDirectory("step-ap-ide-").toFile();
                logger.warn("initialDir is not present, using temporary directory: {}", workDir.getAbsolutePath());
            } else {
                FileUtils.copyDirectory(initialDir, workDir);
                if (!workDir.isDirectory()) {
                    throw new RuntimeException("Something went wrong while initializing directory: " + workDir.getAbsolutePath());
                }
            }
        } else {
            logger.info("Using existing work directory at: " + workDir.getAbsolutePath());
            logger.info("You may delete this directory to start over from scratch");
        }
        useAutomationPackageDirectory(workDir);
    }

    static void useAutomationPackageDirectory(File apDir) throws Exception {
        verifyOrCreateMainAPFile(apDir);
        var fragmentManager = StepUp.READER.getAutomationPackageYamlFragmentManager(apDir);
        Properties properties = new Properties();

        int variant = 1;
        // variant 1:
        // parameters all go into parameters.yml, plans go into separate files in plans/$PLAN_NAME.yml
        // this is because parameters do not have unique names by design.
        if (variant == 1) {
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, Parameter.ENTITY_NAME), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, Parameter.ENTITY_NAME), "parameters.yml");
            // per default, PER_OBJECT is used on all objects?
        }
        // variant 2: simple, everything goes into main descriptor
        if (variant == 2) {
            String mainFile = fragmentManager.descriptorYaml.getFragmentPath().toFile().getName();
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, Parameter.ENTITY_NAME), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, Parameter.ENTITY_NAME), mainFile);
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, YamlPlan.PLANS_ENTITY_NAME), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, YamlPlan.PLANS_ENTITY_NAME), mainFile);
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, "keywords"), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, "keywords"), mainFile);
        }
        fragmentManager.setProperties(properties);
        var automationPackageCollectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);

        CurrentlyOpenedAutomationPackageCollectionFactory.getInstance().setCurrentFactory(automationPackageCollectionFactory);
    }

    private static void verifyOrCreateMainAPFile(File apDir) throws Exception {
        for (String fileName : JavaAutomationPackageArchive.METADATA_FILES) {
            if (new File(apDir, fileName).isFile()) {
                return;
            }
        }
        File descriptor = new File(apDir, JavaAutomationPackageArchive.METADATA_FILES.getFirst());
        logger.info("Initializing AP directory with new descriptor: {}", descriptor.getAbsolutePath());
        PrintWriter pw = new PrintWriter(new FileOutputStream(descriptor));
        pw.println("schemaVersion: 1.0.0");
        pw.println("name: \"My package\""); // TODO: make this configurable somehow
        pw.close();
    }
}
