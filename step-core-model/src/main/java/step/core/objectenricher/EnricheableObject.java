package step.core.objectenricher;

import java.util.Map;

public interface EnricheableObject {

    void addAttribute(String key, String value);

    String getAttribute(String key);

    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);
}
