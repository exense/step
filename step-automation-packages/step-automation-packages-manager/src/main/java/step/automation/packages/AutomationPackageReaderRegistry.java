package step.automation.packages;

import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AutomationPackageReaderRegistry {

    private final Map<String, AutomationPackageReader<? extends AutomationPackageArchive>> readers = new HashMap<>();
    private final String jsonSchemaPath;
    private final AutomationPackageHookRegistry hookRegistry;
    private final AutomationPackageSerializationRegistry automationPackageSerializationRegistry;


    public AutomationPackageReaderRegistry(String jsonSchemaPath, AutomationPackageHookRegistry hookRegistry, AutomationPackageSerializationRegistry automationPackageSerializationRegistry) {
        Objects.requireNonNull(jsonSchemaPath, "The jsonSchemaPath must not be null");
        Objects.requireNonNull(hookRegistry, "The hookRegistry must not be null");
        Objects.requireNonNull(automationPackageSerializationRegistry, "The automationPackageSerializationRegistry must not be null");
        this.jsonSchemaPath = jsonSchemaPath;
        this.hookRegistry = hookRegistry;
        this.automationPackageSerializationRegistry = automationPackageSerializationRegistry;
    }

    public void register(AutomationPackageReader<? extends AutomationPackageArchive> reader) {
        readers.put(reader.getAutomationPackageType(), reader);
    }

    public <T extends AutomationPackageArchive> AutomationPackageReader<T> getReader(T archive) {
        AutomationPackageReader<? extends AutomationPackageArchive> automationPackageReader = readers.get(archive.getType());
        if (automationPackageReader == null) {
            throw new AutomationPackageManagerException("No Automation Package reader found for archive with type " + archive.getType() + ". Supported types are: " + getSupportedTypes());
        }
        //noinspection unchecked
        return (AutomationPackageReader<T>) automationPackageReader;
    }

    public <T extends AutomationPackageArchive> AutomationPackageReader<T> getReaderByType(String type) {
        AutomationPackageReader<? extends AutomationPackageArchive> automationPackageReader = readers.get(type);
        if (automationPackageReader == null) {
            throw new AutomationPackageManagerException("No Automation Package reader found for archive type " + type + ". Supported types are: " + getSupportedTypes());
        }
        //noinspection unchecked
        return (AutomationPackageReader<T>) automationPackageReader;
    }

    public void updateAllReaders(Consumer<AutomationPackageReader<?>> consumer) {
        readers.values().forEach(consumer);
    }

    public <T extends AutomationPackageArchive> AutomationPackageReader<T> getReaderForFile(File file) {
        //noinspection unchecked
        return (AutomationPackageReader<T>)  readers.values().stream().filter(r -> r.isValidForFile(file)).findFirst().orElseThrow(() ->
                new AutomationPackageManagerException("No Automation Package reader found for file " + file.getName() + ". Supported types are: " + getSupportedTypes()));
    }

    public String getSupportedTypes() {
        return readers.values().stream().map(AutomationPackageReader::getSupportedFileTypes).flatMap(List::stream).map(Object::toString).collect(Collectors.joining(", "));
    }

    public String getJsonSchemaPath() {
        return jsonSchemaPath;
    }

    public AutomationPackageHookRegistry getHookRegistry() {
        return hookRegistry;
    }

    public AutomationPackageSerializationRegistry getAutomationPackageSerializationRegistry() {
        return automationPackageSerializationRegistry;
    }
}
