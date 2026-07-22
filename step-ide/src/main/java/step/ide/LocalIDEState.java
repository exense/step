package step.ide;

import ch.exense.commons.app.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.function.Failable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.JavaAutomationPackageArchive;
import step.automation.packages.JavaAutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.collections.AutomationPackageCollectionFactory;
import step.core.execution.ExecutionDiversion;
import step.core.execution.model.ExecutionParameters;
import step.ide.api.IDEExecutorDelegate;
import step.ide.api.IDEExecutorDelegateFactory;
import step.ide.collections.CurrentlyOpenedAutomationPackageCollectionFactory;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plans.parser.yaml.YamlPlan;
import step.resources.ResourceManagerImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class LocalIDEState implements ExecutionDiversion {
    private static final Logger logger = LoggerFactory.getLogger(LocalIDEState.class);
    private static final LocalIDEState instance = new LocalIDEState();

    private final JavaAutomationPackageReader reader;

    private final List<File> directoriesToCleanupOnShutdown = new ArrayList<>();
    private ResourceManagerImpl resourceManager;
    private IDEExecutorDelegateFactory executorDelegateFactory;
    private File currentAutomationPackageDirectory;
    private FileResolver fileResolver;

    public static LocalIDEState get() {
        return instance;
    }

    private LocalIDEState() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        // required for reading parameters, apparently the manager can be null
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, null);
        reader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
    }

    public void setResourceManager(ResourceManagerImpl resourceManager) {
        this.resourceManager = resourceManager;
        logger.debug("Setting resource manager to {}", resourceManager);
    }

    public void useExistingAutomationPackageDirectory(File apDir) throws Exception {
        verifyAPDirectory(apDir);
        useAutomationPackageDirectory(apDir);
    }

    public void useNewAutomationPackageDirectory(File apDir, String apName) throws Exception {
        initializeAPDirectory(apDir, apName);
        useAutomationPackageDirectory(apDir);
    }

    private void useAutomationPackageDirectory(File apDir) throws Exception {
        var fragmentManager = reader.getAutomationPackageYamlFragmentManager(apDir, this.resourceManager);
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
        this.currentAutomationPackageDirectory = apDir;
        this.fileResolver.setUnprefixedRoot(apDir.toPath());
    }

    private void verifyAPDirectory(File apDir) throws Exception {
        for (String fileName : JavaAutomationPackageArchive.METADATA_FILES) {
            if (new File(apDir, fileName).isFile()) {
                return;
            }
        }
        throw new IllegalArgumentException("Directory " + apDir + " does not contain an automation package descriptor");
    }

    private void initializeAPDirectory(File apDir, String apName) throws Exception {
        Objects.requireNonNull(apDir, "apDir must not be null");
        Objects.requireNonNull(apName, "apName must not be null");
        if (!apDir.isDirectory()) {
            String error = String.format("Path %s  is not a usable directory, unable to initialize Automation Package", apDir.getAbsolutePath());
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        if (Objects.requireNonNull(apDir.listFiles()).length != 0) {
            String error = String.format("Directory %s is not empty, refusing to initialize Automation Package", apDir.getAbsolutePath());
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        File descriptor = new File(apDir, JavaAutomationPackageArchive.METADATA_FILES.getFirst());
        logger.info("Initializing AP directory with new descriptor: {}", descriptor.getAbsolutePath());
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(descriptor))) {
            // escape backslashes and quotes
            String yamlName = apName.replace("\\", "\\\\").replace("\"", "\\\"");
            pw.println("schemaVersion: 1.0.0");
            pw.println("name: \"" + yamlName + "\"");
        }
    }


    public File getCurrentAutomationPackageDirectory() {
        return currentAutomationPackageDirectory;
    }


    public String getCurrentAutomationPackageName() {
        if (currentAutomationPackageDirectory == null) {
            return null;
        }
        // FIXME: determine name
        return "FIXME";
    }

    public void closeCurrentAutomationPackage() {
        CurrentlyOpenedAutomationPackageCollectionFactory.getInstance().setCurrentFactory(null);
        this.currentAutomationPackageDirectory = null;
    }

    public void setExecutorDelegateFactory(IDEExecutorDelegateFactory executorDelegateFactory) {
        this.executorDelegateFactory = executorDelegateFactory;
    }

    @Override
    public String divertExecution(ExecutionParameters executionParams) {
        Objects.requireNonNull(currentAutomationPackageDirectory, "currentAutomationPackageDirectory not set; select an AP first");
        logger.info("Launching diverted execution for parameters: {}", Failable.call(() -> new ObjectMapper().writeValueAsString(executionParams)));
        IDEExecutorDelegate executorDelegate = executorDelegateFactory.createDelegate(currentAutomationPackageDirectory, executionParams);
        CompletableFuture<String> executionIdFuture = new CompletableFuture<>();
        CompletableFuture.runAsync((() -> {
            try {
                executorDelegate.executePackageAndFillExecutionId(executionIdFuture);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                executionIdFuture.completeExceptionally(e);
            }
        }));
        String executionId = executionIdFuture.join();
        logger.info("Diverted executionId: {}", executionId);
        return executionId;
    }

    public void setFileResolver(FileResolver fileResolver) {
        this.fileResolver = fileResolver;
    }

    public void addDirectoriesToCleanupOnShutdown(Collection<File> directories) {
        this.directoriesToCleanupOnShutdown.addAll(Objects.requireNonNull(directories));
        if (logger.isDebugEnabled()) {
            for (File directory : directoriesToCleanupOnShutdown) {
                logger.debug("Registering directory for cleanup on shutdown: {}", directory.getAbsolutePath());
            }
        }
    }

    public void onShutdown() {
        logger.info("Shutting down, performing cleanup tasks");
        for (File directory : directoriesToCleanupOnShutdown) {
            if (!directory.isDirectory()) {
                logger.warn("Directory {} is not a usable directory, unable to cleanup", directory.getAbsolutePath());
            }
            try {
                logger.debug("Cleaning up directory {}", directory.getAbsolutePath());
                FileUtils.deleteDirectory(directory);
            } catch (Exception e) {
                logger.error("Error while deleting directory {}", directory.getAbsolutePath(), e);
            }
        }
    }
}
