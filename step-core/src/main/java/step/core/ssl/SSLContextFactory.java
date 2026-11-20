package step.core.ssl;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class SSLContextFactory {

    public static SSLContext nonValidatingSSLContext() {
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new TrustAnyTrustManager()}, new java.security.SecureRandom());
            return sc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SSLContext singleClientCertContext(String certFile, String passwordBase64) {
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(new KeyManager[]{new SingleClientCertKeyManager(certFile, passwordBase64)}, new TrustManager[]{new TrustAnyTrustManager()}, new java.security.SecureRandom());
            return sc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
