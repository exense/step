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
import step.ide.api.IDEExecutionRequest;
import step.ide.api.IDEExecutorDelegate;
import step.ide.api.IDEExecutorDelegateFactory;
import step.ide.collections.CurrentlyOpenedAutomationPackageCollectionFactory;
import step.ide.exceptions.FileExistsException;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plans.parser.yaml.YamlPlan;
import step.resources.ResourceManagerImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class LocalIDEState implements ExecutionDiversion {
    private static final Logger logger = LoggerFactory.getLogger(LocalIDEState.class);
    private static final LocalIDEState instance = new LocalIDEState();

    private static final long EXECUTION_LAUNCH_TIMEOUT_S = 600;

    private final JavaAutomationPackageReader reader;

    private final List<Path> directoriesToCleanupOnShutdown = new CopyOnWriteArrayList<>();
    private ResourceManagerImpl resourceManager;
    private IDEExecutorDelegateFactory executorDelegateFactory;
    private Path currentAutomationPackageDirectory;
    private FileResolver fileResolver;
    private CompletableFuture<Void> startupAwaitFuture;
    private CompletableFuture<Void> shutdownAwaitFuture;
    private Configuration configuration;

    public static LocalIDEState get() {
        return instance;
    }

    private LocalIDEState() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, null);
        reader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
    }

    public void setResourceManager(ResourceManagerImpl resourceManager) {
        this.resourceManager = resourceManager;
        logger.debug("Setting resource manager to {}", resourceManager);
    }

    public void useExistingAutomationPackageDirectory(Path apDir) throws Exception {
        validateExistingAutomationPackageDirectory(apDir);
        useAutomationPackageDirectory(apDir);
    }

    public void useNewAutomationPackageDirectory(Path apDir, String apName) throws Exception {
        initializeAPDirectory(apDir, apName);
        useAutomationPackageDirectory(apDir);
    }

    private void useAutomationPackageDirectory(Path apDir) throws Exception {
        var fragmentManager = reader.getAutomationPackageYamlFragmentManager(apDir.toFile(), this.resourceManager);
        Properties properties = new Properties();

        int variant = 1;
        if (variant == 1) {
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, Parameter.ENTITY_NAME), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());
            properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, Parameter.ENTITY_NAME), "parameters.yml");
        }
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
        this.currentAutomationPackageDirectory = apDir.toAbsolutePath().normalize();
        this.fileResolver.setUnprefixedRoot(apDir);
    }

    private Path findMetadataFile(Path apDirectory) {
        for (String fileName : JavaAutomationPackageArchive.METADATA_FILES) {
            Path metadataFile = apDirectory.resolve(fileName);
            if (Files.isRegularFile(metadataFile)) {
                return metadataFile;
            }
        }
        return null;
    }

    private Path resolveAndCheckBaseDirectory(Path apDirectory, boolean forInitialization) {
        if (apDirectory == null) {
            throw new IllegalArgumentException("Directory path must not be null");
        }
        Path resolvedDir = apDirectory.toAbsolutePath().normalize();

        if (!Files.exists(resolvedDir)) {
            if (forInitialization) {
                return resolvedDir;
            }
            throw new IllegalArgumentException("Path does not exist: " + resolvedDir);
        }

        if (!Files.isDirectory(resolvedDir)) {
            throw new IllegalArgumentException("Path exists but is not a directory: " + resolvedDir);
        }

        if (!Files.isReadable(resolvedDir)) {
            throw new IllegalArgumentException("Directory is not readable: " + resolvedDir);
        }

        // This is an absolute edge case, but in theory we could get away with read-only directories in some cases (not very useful for an editor though).
        if (forInitialization && !Files.isWritable(resolvedDir)) {
            throw new IllegalArgumentException("Directory is not writable: " + resolvedDir);
        }

        return resolvedDir;
    }

    public void validateExistingAutomationPackageDirectory(Path apDirectory) {
        Path resolvedDir = resolveAndCheckBaseDirectory(apDirectory, false);
        if (findMetadataFile(resolvedDir) == null) {
            throw new IllegalArgumentException("Directory " + resolvedDir + " does not contain an automation package descriptor");
        }
    }

    public void validateInitializableAutomationPackageDirectory(Path apDirectory, boolean allowExistingDescriptor) throws FileExistsException {
        Path resolvedDir = resolveAndCheckBaseDirectory(apDirectory, true);
        if (!allowExistingDescriptor) {
            Path existingDescriptor = findMetadataFile(resolvedDir);
            if (existingDescriptor != null) {
                throw new FileExistsException(existingDescriptor);
            }
        }
    }

    private void initializeAPDirectory(Path apDir, String apName) throws Exception {
        Objects.requireNonNull(apDir, "apDir must not be null");

        // 1. Create the directory if it doesn't exist (we allow this from the CLI)
        if (!Files.exists(apDir)) {
            Files.createDirectories(apDir);
            logger.info("Created new Automation Package directory: {}", apDir.toAbsolutePath());
        }

        if (!Files.isDirectory(apDir)) {
            String error = String.format("Path %s is not a usable directory, unable to initialize Automation Package", apDir.toAbsolutePath());
            logger.error(error);
            throw new IllegalArgumentException(error);
        }

        Path descriptor = Objects.requireNonNullElseGet(findMetadataFile(apDir),
            () -> apDir.resolve(JavaAutomationPackageArchive.METADATA_FILES.getFirst())
        );

        logger.info("Initializing AP descriptor: {}", descriptor.toAbsolutePath());

        if (apName == null || apName.isBlank()) {
            Path fileName = apDir.getFileName();
            // Edge case: FS roots (/ or C:) apparently return a null filename
            apName = fileName != null ? fileName.toString() : "root-directory";
        }

        String yamlName = apName.replace("\\", "\\\\").replace("\"", "\\\"");
        String content = "schemaVersion: 1.0.0\nname: \"" + yamlName + "\"\n";
        Files.writeString(descriptor, content);
    }

    public Path getCurrentAutomationPackageDirectory() {
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
        Path apDir = requireCurrentAutomationPackageDirectory();
        String description = executionParams.getDescription();
        List<String> includedPlanNames = (description == null || description.isBlank()) ? List.of() : List.of(description);
        return executeAutomationPackage(new IDEExecutionRequest(apDir, executionParams, includedPlanNames));
    }

    /**
     * Executes an automation package through the configured delegate and returns the id of the launched execution.
     * The package is not necessarily the currently opened one: the AI agent for instance is a packaged automation
     * package of its own, executed against the opened package.
     */
    public String executeAutomationPackage(IDEExecutionRequest request) {
        Objects.requireNonNull(executorDelegateFactory, "No IDEExecutorDelegateFactory set, the IDE was not started through the CLI launcher");
        logger.info("Launching diverted execution of {} (plans: {}) for parameters: {}", request.automationPackage(),
            request.includedPlanNames(), Failable.call(() -> new ObjectMapper().writeValueAsString(request.executionParameters())));
        IDEExecutorDelegate executorDelegate = executorDelegateFactory.createDelegate(request);
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

    public Path requireCurrentAutomationPackageDirectory() {
        Path apDir = currentAutomationPackageDirectory;
        if (apDir == null) {
            throw new IllegalStateException("No automation package is currently opened, please open one first");
        }
        return apDir;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setFileResolver(FileResolver fileResolver) {
        this.fileResolver = fileResolver;
    }

    public void addDirectoriesToCleanupOnShutdown(Collection<Path> directories) {
        this.directoriesToCleanupOnShutdown.addAll(Objects.requireNonNull(directories));
        if (logger.isDebugEnabled()) {
            for (Path directory : directoriesToCleanupOnShutdown) {
                logger.debug("Registering directory for cleanup on shutdown: {}", directory.toAbsolutePath());
            }
        }
    }

    public void setStartupAwaitFuture(CompletableFuture<Void> startupAwaitFuture) {
        this.startupAwaitFuture = startupAwaitFuture;
    }

    public void setShutdownAwaitFuture(CompletableFuture<Void> shutdownAwaitFuture) {
        this.shutdownAwaitFuture = shutdownAwaitFuture;
    }

    public void onStartupFinished() {
        if (startupAwaitFuture != null) {
            startupAwaitFuture.complete(null);
            startupAwaitFuture = null;
        }
    }

    public void onShutdown() {
        logger.info("Shutting down, performing cleanup tasks");
        for (Path directory : directoriesToCleanupOnShutdown) {
            if (!Files.isDirectory(directory)) {
                logger.warn("Directory {} is not a usable directory, unable to cleanup", directory.toAbsolutePath());
            }
            try {
                logger.debug("Cleaning up directory {}", directory.toAbsolutePath());
                FileUtils.deleteDirectory(directory.toFile());
            } catch (Exception e) {
                logger.error("Error while deleting directory {}", directory.toAbsolutePath(), e);
            }
        }
        // We're intentionally doing the cleanup above even on error, otherwise we would leak temporary directories.
        if (startupAwaitFuture != null) {
            startupAwaitFuture.completeExceptionally(new RuntimeException("Unexpected shutdown while starting up. Consult the log for error details."));
        }
        if (shutdownAwaitFuture != null) {
            logger.debug("Completing shutdown-await future");
            shutdownAwaitFuture.complete(null);
        }
    }

}
