package step.core.proxy;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for IPv6 proxy hosts. IPv6 literals are deliberately kept in their bracketed form (e.g. "[::1]") in the
 * proxy's (unresolved) address: the address is used to build proxy URIs (see {@link ProxyUri}), where the brackets
 * are mandatory, and Java's address resolution accepts the bracketed form as well (RFC 2732, see
 * {@link InetAddress#getByName(String)}). Stripping the brackets would result in invalid URIs like "http://::1:3128".
 */
public class ExtendedProxyTest {

    @Test
    public void testIpv6FromPacEntry() throws Exception {
        assertIpv6Proxy(ExtendedProxy.parseFromPacEntry("PROXY [::1]:3128"), "http://[::1]:3128");
        assertIpv6Proxy(ExtendedProxy.parseFromPacEntry("HTTPS [::1]:3128"), "https://[::1]:3128");
    }

    @Test
    public void testIpv6FromUrl() throws Exception {
        assertIpv6Proxy(ExtendedProxy.parseFromUrl("http://[::1]:3128"), "http://[::1]:3128");
        assertIpv6Proxy(ExtendedProxy.parseFromUrl("https://[::1]:3128"), "https://[::1]:3128");
    }

    private static void assertIpv6Proxy(Proxy proxy, String expectedProxyUri) throws Exception {
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        assertEquals("[::1]", address.getHostString());
        assertEquals(3128, address.getPort());

        // The proxy URI must keep the brackets, and must be a valid URI
        String proxyUri = ProxyUri.fromProxyList(List.of(proxy));
        assertEquals(expectedProxyUri, proxyUri);
        URI uri = new URI(proxyUri);
        assertEquals("[::1]", uri.getHost());
        assertEquals(3128, uri.getPort());

        // The bracketed form resolves to the same address as the bare literal (no actual DNS lookup involved)
        InetAddress resolved = InetAddress.getByName(address.getHostString());
        assertEquals(InetAddress.getByName("::1"), resolved);
        assertTrue(resolved.isLoopbackAddress());
    }
}
