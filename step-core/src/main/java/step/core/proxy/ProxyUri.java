package step.core.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

public class ProxyUri {
    private static final Logger logger = LoggerFactory.getLogger(ProxyUri.class);

    public static String fromProxyList(List<Proxy> proxies) {
        if (proxies == null || proxies.isEmpty()) {
            logger.warn("Empty proxy list, returning null to indicate no proxy");
            return null;
        }
        for (Proxy proxy : proxies) {
            if (proxy == null || proxy.type() == Proxy.Type.DIRECT) {
                return null;
            } else if (proxy.type() == Proxy.Type.HTTP && proxy.address() instanceof InetSocketAddress) {
                String scheme = proxy instanceof ExtendedProxy ? ((ExtendedProxy) proxy).getUrlScheme() : "http";
                InetSocketAddress a = (InetSocketAddress) proxy.address();
                return scheme + "://" + a.getHostString() + ":" + a.getPort();
            } else {
                logger.warn("Unsupported proxy, skipping: {}", proxy);
            }
        }
        logger.warn("No suitable proxy found, returning null to indicate no proxy");
        return null;
    }

    public static String fromProxySelector(ProxySelector proxySelector, URI targetUri) {
        if (proxySelector == null || targetUri == null) {
            logger.warn("Null proxy selector or null URI, returning null to indicate no proxy");
            return null;
        }
        return fromProxyList(proxySelector.select(targetUri));
    }
}
