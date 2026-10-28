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
        PAC
    }

    public Type type;
    public String url;
    public String pacMyIpAddress;

    private static final Pattern PAC_URL_PATTERN =
        Pattern.compile("(?i)^https?://[^@\\s]+$");

    public static ProxySelector toProxySelector(ProxyConfiguration configuration) {
        if (configuration == null) {
            return ProxySelector.of(null);
        }

        switch (configuration.type) {
            case STATIC: {
                requireNonEmpty(configuration.url, "HTTP(S) proxy URL");
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

            case PAC:
            default: {
                requireNonEmpty(configuration.url, "PAC URL");
                if (!PAC_URL_PATTERN.matcher(configuration.url).matches()) {
                    throw new IllegalArgumentException(
                        "Invalid PAC URL (must be http(s) and contain no credentials): " + configuration.url);
                }
                if (configuration.pacMyIpAddress != null) {
                    System.setProperty(PacScriptMethods.OVERRIDE_LOCAL_IP, configuration.pacMyIpAddress);
                }
                return new PacProxySelector(new UrlPacScriptSource(configuration.url));
            }
        }
    }

    private static void requireNonEmpty(String s, String name) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }
}
