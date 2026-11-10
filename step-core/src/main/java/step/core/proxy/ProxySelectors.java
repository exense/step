package step.core.proxy;

import ch.exense.commons.app.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ProxySelector;
import java.util.EnumMap;
import java.util.Objects;

/**
 * This class uses a singleton pattern because:
 * 1. The proxy settings currently only affect calls performed directly from the single controller instance
 * 2. Classloader or context separation is not required, these are global configuration settings
 * 3. The settings need to be accessed from a variety of (logical) contexts such as repositories or notification events,
 * where an ExecutionContext (or other AbstractContext) isn't necessarily available; even if it were, the machinery
 * for passing the information from context to context would be overkill compared to just keeping it global
 * 4. Alternative approaches (e.g., parsing the configuration each time on demand) is inefficient (in particular for PAC,
 * it would need to re-fetch the remote .pac file on every use)
 */
public class ProxySelectors {
    public static final String CONFIGURATION_KEY_PROXIES = "proxies";
    public static final String CONFIGURATION_KEY_PACMYIPADDRESS = "pacMyIpAddress";

    private static final Logger logger = LoggerFactory.getLogger(ProxySelectors.class);

    public static final ProxySelector NO_PROXY = ProxySelector.of(null);

    public enum UsageContext {
        DEFAULT,
        XRAY,
        WEBHOOK
    }

    private static ProxySelectors INSTANCE = null;

    public static ProxySelectors getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ProxySelectors instance has not been initialized");
        }
        return INSTANCE;
    }

    public static void initialize(Configuration stepConfiguration) {
        INSTANCE = fromStepConfiguration(stepConfiguration);
    }

    private final EnumMap<UsageContext, ProxySelector> selectors;

    private ProxySelectors(ProxyConfigurations configs) {
        selectors = new EnumMap<>(UsageContext.class);
        if (configs == null || configs.isEmpty()) {
            logger.debug("No proxy configurations were provided, creating a single default non-proxying selector");
            // actual creation is below in the fallback branch
        } else {
            configs.forEach((key, value) -> selectors.put(key, ProxyConfiguration.toProxySelector(value, key.toString())));
        }
        if (!selectors.containsKey(UsageContext.DEFAULT)) {
            // If not already defined, ensure we always have a DEFAULT entry (direct connection, i.e. no proxy)
            selectors.put(UsageContext.DEFAULT, ProxySelectors.NO_PROXY);
        }
    }

    public static void setPacMyIpAddress(String pacMyIpAddress) {
        // Because of how the PAC implementation works (with static classes/methods and a system property), only a single
        // definition of myIpAddress is possible (even if technically, multiple PAC configurations could be used)
        if (pacMyIpAddress != null && !pacMyIpAddress.isBlank()) {
            System.setProperty(PacScriptMethods.OVERRIDE_LOCAL_IP, pacMyIpAddress);
        }
    }

    static ProxySelectors fromStepConfiguration(Configuration stepConfiguration) {
        String proxiesString = stepConfiguration.getProperty(CONFIGURATION_KEY_PROXIES, null);
        String pacMyIpAddress = stepConfiguration.getProperty(CONFIGURATION_KEY_PACMYIPADDRESS, null);
        if (proxiesString == null) {
            logger.debug("Configuration key {} not found in step.properties, no proxies will be used", CONFIGURATION_KEY_PROXIES);
        }
        return fromConfigurationStrings(proxiesString, pacMyIpAddress);
    }

    static ProxySelectors fromConfigurationStrings(String proxiesJsonString, String pacMyIpAddress) {
        try {
            ProxySelectors selectors;
            if (proxiesJsonString == null || proxiesJsonString.isBlank()) {
                selectors = new ProxySelectors(null);
            } else {
                ProxyConfigurations configs = new ObjectMapper().readValue(proxiesJsonString, ProxyConfigurations.class);
                selectors = new ProxySelectors(configs);
            }
            // The method takes the argument for API consistency, but the actual setting is system-wide.
            setPacMyIpAddress(pacMyIpAddress);
            return selectors;
        } catch (Exception ex) {
            String error = String.format("Invalid '%s' configuration in step.properties", CONFIGURATION_KEY_PROXIES);
            logger.error(error, ex);
            throw new IllegalArgumentException(error, ex);
        }
    }

    public ProxySelector getProxySelector(UsageContext usageContext) {
        Objects.requireNonNull(usageContext, "usageContext must not be null");
        ProxySelector result = selectors.get(usageContext);
        // DEFAULT is guaranteed to be defined as a fallback.
        return result != null ? result : selectors.get(UsageContext.DEFAULT);
    }
}
