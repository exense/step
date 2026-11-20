package step.core.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtendedProxy extends Proxy {
    // This is the extended syntax as documented here:
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Proxy_servers_and_tunneling/Proxy_Auto-Configuration_PAC_file
    public enum Scheme {HTTP, HTTPS, SOCKS, SOCKS4, SOCKS5, DIRECT}

    private final Scheme scheme;

    public ExtendedProxy(Type type, SocketAddress sa, Scheme scheme) {
        super(type, sa);
        this.scheme = Objects.requireNonNull(scheme);
    }

    @Override
    public String toString() {
        return "(" + scheme + ") " + super.toString();
    }

    public Scheme getScheme() {
        return scheme;
    }

    public String getUrlScheme() {
        // let's keep this simple
        return scheme.toString().toLowerCase(Locale.ROOT);
    }

    /* ===================== Parsing ===================== */

    // http(s)://host:port (no creds, no path/query); IPv6 allowed via [addr]
    private static final Pattern URL_PROXY =
        Pattern.compile("(?i)^(https?)://(\\[[^]]+]|[\\w.-]+):(\\d{1,5})$");

    public static ExtendedProxy parseFromUrl(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Empty proxy URL");
        Matcher m = URL_PROXY.matcher(url.trim());
        if (!m.matches())
            throw new IllegalArgumentException("Invalid proxy URL (expect http(s)://host:port): " + url);

        Scheme scheme = Scheme.valueOf(m.group(1).toUpperCase(Locale.ROOT)); // HTTP or HTTPS
        String host = m.group(2);
        int port = parsePort(m.group(3), url);

        Type type = Type.HTTP; // talk HTTP(S) to proxy endpoint
        return new ExtendedProxy(type, InetSocketAddress.createUnresolved(host, port), scheme);
    }

    public static Proxy parseFromPacEntry(String pacEntry) {
        if (pacEntry == null) throw new IllegalArgumentException("Null PAC entry");
        String s = pacEntry.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Empty PAC entry");

        if (s.equalsIgnoreCase(Scheme.DIRECT.toString())) {
            return Proxy.NO_PROXY;
        }

        String[] parts = s.split("\\s+", 2);
        if (parts.length != 2)
            throw new IllegalArgumentException("Invalid PAC entry (expecting KIND host:port): " + s);

        String kind = parts[0].trim().toUpperCase(Locale.ROOT);
        if (kind.equals("PROXY")) kind = Scheme.HTTP.name(); // PAC alias

        String hp = parts[1].trim();
        String host, portStr;

        if (hp.startsWith("[")) { // IPv6 [addr]:port
            int end = hp.indexOf(']');
            if (end < 0 || end + 2 > hp.length() || hp.charAt(end + 1) != ':')
                throw new IllegalArgumentException("Invalid IPv6 host:port in PAC entry: " + s);
            host = hp.substring(0, end + 1);
            portStr = hp.substring(end + 2);
        } else {
            int idx = hp.lastIndexOf(':');
            if (idx < 0) {
                host = hp;
                portStr = "80";
            } else {
                host = hp.substring(0, idx);
                portStr = hp.substring(idx + 1);
            }
        }
        host = host.trim();
        if (host.isEmpty()) throw new IllegalArgumentException("Empty host in PAC entry: " + s);

        int port = parsePort(portStr, s);

        Scheme scheme = parseScheme(kind, s);
        if (scheme == Scheme.DIRECT) {
            throw new IllegalArgumentException("Invalid PAC entry: DIRECT must not contain host:port specification: " + s);
        }
        Type type = (scheme == Scheme.SOCKS || scheme == Scheme.SOCKS4 || scheme == Scheme.SOCKS5)
            ? Type.SOCKS : Type.HTTP;

        return new ExtendedProxy(type, InetSocketAddress.createUnresolved(host, port), scheme);
    }

    /* ===================== Helpers ===================== */

    private static int parsePort(String p, String ctx) {
        try {
            int port = Integer.parseInt(p.trim());
            if (port < 1 || port > 65535) throw new IllegalArgumentException();
            return port;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid port (" + p + ") in: " + ctx);
        }
    }

    private static Scheme parseScheme(String s, String ctx) {
        try {
            return Scheme.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid scheme (" + s + ") in: " + ctx);
        }
    }
}
