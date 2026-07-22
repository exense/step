package step.cli;

import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

public class TestCommandFactory implements CommandLine.IFactory {
    private final Map<Class<?>, Object> instances = new HashMap<>();

    public TestCommandFactory register(Class<?> cls, Object instance) {
        instances.put(cls, instance);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> K create(Class<K> cls) throws Exception {
        // If we registered a mock/test double for this class, return it
        if (instances.containsKey(cls)) {
            return (K) instances.get(cls);
        }
        // Otherwise, let picocli instantiate it normally
        return CommandLine.defaultFactory().create(cls);
    }
}
