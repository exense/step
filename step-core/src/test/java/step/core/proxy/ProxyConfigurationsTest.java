package step.core.proxy;

import ch.exense.commons.app.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.core.proxy.ProxySelectors.UsageContext;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests the parsing of the global proxy configurations and the selection of the proxy selector per call context,
 * in particular the fallback to DEFAULT and the overrides. These tests do not perform any actual network access:
 * static and direct selectors are checked through their selection result, while PAC selectors (which download
 * their script lazily) are only checked for their type and identity.
 */
public class ProxyConfigurationsTest {

    private static final URI TARGET = URI.create("https://jira.example.com/rest/api/2/issue/ABC-1");

    private static final String STATIC_DEFAULT = "{\"type\":\"STATIC\",\"url\":\"http://default-proxy.example.com:3128\"}";
    private static final String STATIC_XRAY = "{\"type\":\"STATIC\",\"url\":\"https://xray-proxy.example.com:8443\"}";
    private static final String PAC_DEFAULT = "{\"type\":\"PAC\",\"url\":\"http://pac.example.com/default.pac\"}";
    private static final String PAC_XRAY = "{\"type\":\"PAC\",\"url\":\"https://pac.example.com/xray.pac\"}";
    private static final String DIRECT = "{\"type\":\"DIRECT\"}";

    private String previousLocalIpOverride;

    @Before
    public void saveLocalIpOverride() {
        previousLocalIpOverride = System.getProperty(PacScriptMethods.OVERRIDE_LOCAL_IP);
        System.clearProperty(PacScriptMethods.OVERRIDE_LOCAL_IP);
    }

    @After
    public void restoreLocalIpOverride() {
        if (previousLocalIpOverride == null) {
            System.clearProperty(PacScriptMethods.OVERRIDE_LOCAL_IP);
        } else {
            System.setProperty(PacScriptMethods.OVERRIDE_LOCAL_IP, previousLocalIpOverride);
        }
    }

    /* ===================== No configuration at all ===================== */

    @Test
    public void testMissingPropertyInStepConfiguration() {
        ProxySelectors selectors = ProxySelectors.fromStepConfiguration(new Configuration());
        assertAllDirectAndSameAsDefault(selectors);
    }

    @Test
    public void testNullConfiguration() {
        assertAllDirectAndSameAsDefault(ProxySelectors.fromConfigurationStrings(null, null));
    }

    @Test
    public void testBlankConfiguration() {
        assertAllDirectAndSameAsDefault(ProxySelectors.fromConfigurationStrings("  ", null));
    }

    @Test
    public void testEmptyJsonObject() {
        assertAllDirectAndSameAsDefault(ProxySelectors.fromConfigurationStrings("{}", null));
    }

    /* ===================== Default only ===================== */

    @Test
    public void testStaticDefaultOnlyAppliesToAllContexts() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + STATIC_DEFAULT + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertStatic(defaultSelector, ExtendedProxy.Scheme.HTTP, "default-proxy.example.com", 3128);
        for (UsageContext context : UsageContext.values()) {
            assertSame("Context " + context + " must fall back to DEFAULT", defaultSelector, selectors.getProxySelector(context));
        }
    }

    @Test
    public void testPacDefaultOnlyAppliesToAllContexts() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + PAC_DEFAULT + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertPac(defaultSelector);
        for (UsageContext context : UsageContext.values()) {
            assertSame("Context " + context + " must fall back to DEFAULT", defaultSelector, selectors.getProxySelector(context));
        }
    }

    @Test
    public void testExplicitDirectDefaultOnly() {
        assertAllDirectAndSameAsDefault(parse("{\"DEFAULT\":" + DIRECT + "}"));
    }

    /* ===================== No default ===================== */

    @Test
    public void testNoDefaultWithStaticOverride() {
        ProxySelectors selectors = parse("{\"XRAY\":" + STATIC_XRAY + "}");
        assertStatic(selectors.getProxySelector(UsageContext.XRAY), ExtendedProxy.Scheme.HTTPS, "xray-proxy.example.com", 8443);

        // DEFAULT is implicitly defined as direct, and other contexts fall back to it
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertDirect(defaultSelector);
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    @Test
    public void testNoDefaultWithPacOverride() {
        ProxySelectors selectors = parse("{\"XRAY\":" + PAC_XRAY + "}");
        assertPac(selectors.getProxySelector(UsageContext.XRAY));

        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertDirect(defaultSelector);
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    /* ===================== Default with overrides ===================== */

    @Test
    public void testStaticDefaultWithStaticOverride() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":" + STATIC_XRAY + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertStatic(defaultSelector, ExtendedProxy.Scheme.HTTP, "default-proxy.example.com", 3128);
        assertStatic(selectors.getProxySelector(UsageContext.XRAY), ExtendedProxy.Scheme.HTTPS, "xray-proxy.example.com", 8443);
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    @Test
    public void testPacDefaultWithStaticOverride() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + PAC_DEFAULT + ",\"XRAY\":" + STATIC_XRAY + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertPac(defaultSelector);
        assertStatic(selectors.getProxySelector(UsageContext.XRAY), ExtendedProxy.Scheme.HTTPS, "xray-proxy.example.com", 8443);
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    @Test
    public void testStaticDefaultWithPacOverride() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":" + PAC_XRAY + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertStatic(defaultSelector, ExtendedProxy.Scheme.HTTP, "default-proxy.example.com", 3128);
        assertPac(selectors.getProxySelector(UsageContext.XRAY));
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    @Test
    public void testPacDefaultWithPacOverride() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + PAC_DEFAULT + ",\"XRAY\":" + PAC_XRAY + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        ProxySelector xraySelector = selectors.getProxySelector(UsageContext.XRAY);
        assertPac(defaultSelector);
        assertPac(xraySelector);
        assertNotSame(defaultSelector, xraySelector);
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    @Test
    public void testAllContextsOverridden() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":" + STATIC_XRAY + ",\"WEBHOOK\":" + PAC_XRAY + "}");
        assertStatic(selectors.getProxySelector(UsageContext.DEFAULT), ExtendedProxy.Scheme.HTTP, "default-proxy.example.com", 3128);
        assertStatic(selectors.getProxySelector(UsageContext.XRAY), ExtendedProxy.Scheme.HTTPS, "xray-proxy.example.com", 8443);
        assertPac(selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    @Test
    public void testExplicitDirectDefaultWithOverride() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + DIRECT + ",\"XRAY\":" + STATIC_XRAY + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertDirect(defaultSelector);
        assertStatic(selectors.getProxySelector(UsageContext.XRAY), ExtendedProxy.Scheme.HTTPS, "xray-proxy.example.com", 8443);
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    /* ===================== Default with explicit non-proxy overrides ===================== */

    @Test
    public void testStaticDefaultWithExplicitDirectOverride() {
        assertDirectOverrideOfStaticDefault(DIRECT);
    }

    @Test
    public void testPacDefaultWithExplicitDirectOverride() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + PAC_DEFAULT + ",\"XRAY\":" + DIRECT + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertPac(defaultSelector);
        assertDirect(selectors.getProxySelector(UsageContext.XRAY));
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    @Test
    public void testDirectTypeIgnoresUrl() {
        assertDirectOverrideOfStaticDefault("{\"type\":\"DIRECT\",\"url\":\"http://ignored.example.com:3128\"}");
    }

    private void assertDirectOverrideOfStaticDefault(String xrayOverride) {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":" + xrayOverride + "}");
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertStatic(defaultSelector, ExtendedProxy.Scheme.HTTP, "default-proxy.example.com", 3128);
        assertDirect(selectors.getProxySelector(UsageContext.XRAY));
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
    }

    /* ===================== Step configuration integration ===================== */

    @Test
    public void testFromStepConfiguration() {
        Configuration configuration = new Configuration();
        configuration.putProperty(ProxySelectors.CONFIGURATION_KEY_PROXIES, "{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":" + DIRECT + "}");
        configuration.putProperty(ProxySelectors.CONFIGURATION_KEY_PACMYIPADDRESS, "10.20.30.40");
        ProxySelectors selectors = ProxySelectors.fromStepConfiguration(configuration);
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertStatic(defaultSelector, ExtendedProxy.Scheme.HTTP, "default-proxy.example.com", 3128);
        assertDirect(selectors.getProxySelector(UsageContext.XRAY));
        assertSame(defaultSelector, selectors.getProxySelector(UsageContext.WEBHOOK));
        assertEquals("10.20.30.40", System.getProperty(PacScriptMethods.OVERRIDE_LOCAL_IP));
    }

    /* ===================== pacMyIpAddress ===================== */

    @Test
    public void testPacMyIpAddressIsSet() {
        ProxySelectors.fromConfigurationStrings("{\"DEFAULT\":" + PAC_DEFAULT + "}", "10.20.30.40");
        assertEquals("10.20.30.40", System.getProperty(PacScriptMethods.OVERRIDE_LOCAL_IP));
    }

    @Test
    public void testPacMyIpAddressIsSetEvenWithoutProxies() {
        ProxySelectors.fromConfigurationStrings(null, "10.20.30.40");
        assertEquals("10.20.30.40", System.getProperty(PacScriptMethods.OVERRIDE_LOCAL_IP));
    }

    @Test
    public void testPacMyIpAddressNullOrBlankIsIgnored() {
        ProxySelectors.fromConfigurationStrings("{\"DEFAULT\":" + PAC_DEFAULT + "}", null);
        assertNull(System.getProperty(PacScriptMethods.OVERRIDE_LOCAL_IP));
        ProxySelectors.fromConfigurationStrings("{\"DEFAULT\":" + PAC_DEFAULT + "}", " ");
        assertNull(System.getProperty(PacScriptMethods.OVERRIDE_LOCAL_IP));
    }

    /* ===================== Invalid configurations ===================== */

    @Test
    public void testInvalidJson() {
        assertInvalid("{\"DEFAULT\":");
    }

    @Test
    public void testUnknownCallContext() {
        assertInvalid("{\"jira-xray\":" + STATIC_DEFAULT + "}");
    }

    @Test
    public void testUnknownType() {
        assertInvalid("{\"DEFAULT\":{\"type\":\"SOCKS\",\"url\":\"http://proxy.example.com:1080\"}}");
        // NONE was the previous name of DIRECT
        assertInvalid("{\"DEFAULT\":{\"type\":\"NONE\"}}");
    }

    @Test
    public void testMissingType() {
        // An explicitly defined entry must explicitly define its type, it is neither interpreted as DIRECT
        // nor does it fall back to DEFAULT
        assertInvalid("{\"DEFAULT\":{}}");
        assertInvalid("{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":{}}");
        assertInvalid("{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":{\"url\":\"http://xray-proxy.example.com:3128\"}}");
    }

    @Test
    public void testNullEntry() {
        // Same as above: a null entry is neither interpreted as DIRECT nor does it fall back to DEFAULT
        assertInvalid("{\"DEFAULT\":null}");
        assertInvalid("{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":null}");
    }

    @Test
    public void testUnknownField() {
        // pacMyIpAddress used to be part of the proxy definition, but is now a separate top-level setting
        assertInvalid("{\"DEFAULT\":{\"type\":\"PAC\",\"url\":\"http://pac.example.com/default.pac\",\"pacMyIpAddress\":\"10.20.30.40\"}}");
    }

    @Test
    public void testStaticWithoutUrl() {
        assertInvalid("{\"DEFAULT\":{\"type\":\"STATIC\"}}");
        assertInvalid("{\"DEFAULT\":{\"type\":\"STATIC\",\"url\":\" \"}}");
    }

    @Test
    public void testStaticWithInvalidUrl() {
        assertInvalid("{\"DEFAULT\":{\"type\":\"STATIC\",\"url\":\"http://proxy.example.com\"}}");
        assertInvalid("{\"DEFAULT\":{\"type\":\"STATIC\",\"url\":\"socks://proxy.example.com:1080\"}}");
        assertInvalid("{\"DEFAULT\":{\"type\":\"STATIC\",\"url\":\"http://user:pass@proxy.example.com:3128\"}}");
    }

    @Test
    public void testPacWithoutUrl() {
        assertInvalid("{\"DEFAULT\":{\"type\":\"PAC\"}}");
    }

    @Test
    public void testPacWithInvalidUrl() {
        assertInvalid("{\"DEFAULT\":{\"type\":\"PAC\",\"url\":\"file:///etc/proxy.pac\"}}");
        assertInvalid("{\"DEFAULT\":{\"type\":\"PAC\",\"url\":\"http://user:pass@pac.example.com/proxy.pac\"}}");
    }

    @Test
    public void testInvalidOverrideFailsEvenWithValidDefault() {
        assertInvalid("{\"DEFAULT\":" + STATIC_DEFAULT + ",\"XRAY\":{\"type\":\"STATIC\"}}");
    }

    @Test
    public void testNullCallContextIsRejected() {
        ProxySelectors selectors = parse("{\"DEFAULT\":" + STATIC_DEFAULT + "}");
        assertThrows(NullPointerException.class, () -> selectors.getProxySelector(null));
    }

    /* ===================== Helpers ===================== */

    private static ProxySelectors parse(String json) {
        return ProxySelectors.fromConfigurationStrings(json, null);
    }

    private static void assertInvalid(String json) {
        assertThrows(IllegalArgumentException.class, () -> parse(json));
    }

    private static void assertAllDirectAndSameAsDefault(ProxySelectors selectors) {
        ProxySelector defaultSelector = selectors.getProxySelector(UsageContext.DEFAULT);
        assertDirect(defaultSelector);
        for (UsageContext context : UsageContext.values()) {
            assertSame("Context " + context + " must fall back to DEFAULT", defaultSelector, selectors.getProxySelector(context));
        }
    }

    private static void assertDirect(ProxySelector selector) {
        assertEquals(List.of(Proxy.NO_PROXY), selector.select(TARGET));
    }

    private static void assertStatic(ProxySelector selector, ExtendedProxy.Scheme scheme, String host, int port) {
        List<Proxy> proxies = selector.select(TARGET);
        assertEquals(1, proxies.size());
        assertTrue("Expected an ExtendedProxy, got " + proxies.getFirst(), proxies.getFirst() instanceof ExtendedProxy);
        ExtendedProxy proxy = (ExtendedProxy) proxies.getFirst();
        assertEquals(Proxy.Type.HTTP, proxy.type());
        assertEquals(scheme, proxy.getScheme());
        assertEquals(InetSocketAddress.createUnresolved(host, port), proxy.address());
    }

    private static void assertPac(ProxySelector selector) {
        // Only the type is checked; calling select() would trigger the download of the PAC script
        assertTrue("Expected a PacProxySelector, got " + selector, selector instanceof PacProxySelector);
    }
}
