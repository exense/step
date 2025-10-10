package step.automation.packages;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AutomationPackageReaderRegistry {

    private final Map<AutomationPackageArchiveType, AutomationPackageReader> readers = new HashMap<>();

    public AutomationPackageReaderRegistry() {
    }

    public void register(AutomationPackageReader reader) {
        readers.put(reader.getReaderForAutomationPackageType(), reader);
    }

    public AutomationPackageReader getReader(AutomationPackageArchive archive) {
        return readers.get(archive.getType());
    }


    public AutomationPackageReader getReaderByType(AutomationPackageArchiveType type) {
        return readers.get(type);
    }

    public void updateAllReaders(Consumer<AutomationPackageReader> consumer) {
        readers.values().forEach(consumer);
    }
}
