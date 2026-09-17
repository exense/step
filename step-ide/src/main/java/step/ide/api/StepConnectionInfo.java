package step.ide.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public record StepConnectionInfo(String url, String projectName, String token) {
    public StepConnectionInfo {
        Objects.requireNonNull(url, "url must not be null");
    }

    public static StepConnectionInfo LOCAL = new StepConnectionInfo("http://localhost:8080", null, null);

    public static Map<String, String> toConfigurationMap(StepConnectionInfo info) {
        HashMap<String, String> map = new HashMap<>();
        if (info != null) {
            map.put("ide.stepconnection.cliprops.url", info.url);
            map.put("ide.stepconnection.cliprops.projectname", info.projectName);
            map.put("ide.stepconnection.cliprops.token", info.token);
        }
        return map;
    }

    public static StepConnectionInfo fromCliProperties(Properties props) {
        String url = props.getProperty("stepUrl");
        if (url == null) {
            return null;
        }
        return new StepConnectionInfo(url, props.getProperty("projectName"), props.getProperty("token"));
    }

}
