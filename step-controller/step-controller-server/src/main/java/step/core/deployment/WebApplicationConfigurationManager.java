package step.core.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class WebApplicationConfigurationManager {

    private final List<Function<Session, Map<String, String>>> hooks = new ArrayList<>();

    public boolean registerHook(Function<Session, Map<String, String>> sessionMapFunction) {
        return hooks.add(sessionMapFunction);
    }

    public Map<String, String> getConfiguration(Session session) {
        Map<String, String> result = new HashMap<>();
        hooks.forEach(h -> {
            Map<String, String> hookProperties = h.apply(session);
            result.putAll(hookProperties);
        });
        return result;
    }
}
