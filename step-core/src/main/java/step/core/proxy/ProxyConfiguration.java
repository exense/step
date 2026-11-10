package step.core.proxy;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

public class ProxyConfiguration {
    public enum Type {
        STATIC,
        PAC,
        DIRECT
    }

    public Type type;
    public String url;

    private static final Pattern PAC_URL_PATTERN =
        Pattern.compile("(?i)^https?://[^@\\s]+$");

    public static ProxySelector toProxySelector(ProxyConfiguration configuration, String configName) {
        // An explicitly defined entry must also explicitly define its type; a missing type is most likely a mistake
        // (e.g. a forgotten field, or an unfinished placeholder), so we don't silently interpret it as DIRECT.
        if (configuration == null) {
            throw new IllegalArgumentException("Proxy configuration '" + configName + "' must not be null; use type " + Type.DIRECT + " for direct connections");
        }
        if (configuration.type == null) {
            throw new IllegalArgumentException("Proxy configuration '" + configName + "': field 'type' must be set; use type " + Type.DIRECT + " for direct connections");
        }

        switch (configuration.type) {
            case STATIC: {
                requireNonEmpty(configuration.url, "HTTP(S) proxy URL", configName);
                ExtendedProxy proxy = ExtendedProxy.parseFromUrl(configuration.url);
                return new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {
                        return List.of(proxy);
                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                        // unused
                    }
                };
            }

            case PAC: {
                requireNonEmpty(configuration.url, "PAC URL", configName);
                if (!PAC_URL_PATTERN.matcher(configuration.url).matches()) {
                    throw new IllegalArgumentException(
                        "Invalid PAC URL (must be http(s) and contain no credentials): " + configuration.url);
                }
                return new PacProxySelector(new UrlPacScriptSource(configuration.url));
            }

            case DIRECT:
                return ProxySelectors.NO_PROXY;
            default: {
                // this cannot happen if all enum cases are properly handled
                throw new IllegalStateException("Invalid proxy type: " + configuration.type);
            }
        }
    }

    private static void requireNonEmpty(String s, String name, String configName) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Proxy configuration '" + configName + "': field '" + name + "' must not be empty");
        }
    }
}
