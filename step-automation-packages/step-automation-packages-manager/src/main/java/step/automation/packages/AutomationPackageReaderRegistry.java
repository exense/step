package step.automation.packages;

import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AutomationPackageReaderRegistry {

    private final Map<String, AutomationPackageReader<? extends AutomationPackageArchive>> readers = new HashMap<>();
    private final String jsonSchemaPath;
    private final AutomationPackageHookRegistry hookRegistry;
    private final AutomationPackageSerializationRegistry automationPackageSerializationRegistry;


    public AutomationPackageReaderRegistry(String jsonSchemaPath, AutomationPackageHookRegistry hookRegistry, AutomationPackageSerializationRegistry automationPackageSerializationRegistry) {
        this.jsonSchemaPath = jsonSchemaPath;
        this.hookRegistry = hookRegistry;
        this.automationPackageSerializationRegistry = automationPackageSerializationRegistry;
    }

    public void register(AutomationPackageReader<? extends AutomationPackageArchive> reader) {
        readers.put(reader.getAutomationPackageType(), reader);
    }

    public <T extends AutomationPackageArchive> AutomationPackageReader<?> getReader(T archive) {
        return readers.get(archive.getType());
    }

    public AutomationPackageReader<? extends AutomationPackageArchive> getReaderByType(String type) {
        return readers.get(type);
    }

    public void updateAllReaders(Consumer<AutomationPackageReader<?>> consumer) {
        readers.values().forEach(consumer);
    }

    public AutomationPackageReader<?> getReaderForFile(File file) {
        return readers.values().stream().filter(r -> r.isValidForFile(file)).findFirst().orElseThrow(() ->
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
