package step.core.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509KeyManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;

public class SingleClientCertKeyManager implements X509KeyManager {
    private static final Logger logger = LoggerFactory.getLogger(SingleClientCertKeyManager.class);

    private final KeyStore store;

    private final String alias;
    private final String keyAlgorithm;
    private final String passwordB64;

    public SingleClientCertKeyManager(String keystoreFilename, String passwordBase64) throws Exception {
        this(loadKeyStore(keystoreFilename, passwordBase64), passwordBase64);
    }

    private static KeyStore loadKeyStore(String keystoreFilename, String passwordBase64) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(new FileInputStream(keystoreFilename), decodeBase64(passwordBase64).toCharArray());
        return store;
    }

    public static String decodeBase64(String input) {
        return new String(Base64.getDecoder().decode(input));
    }

    public SingleClientCertKeyManager(KeyStore store, String passwordB64) throws Exception {
        this.store = store;
        this.passwordB64 = passwordB64;

        Enumeration<String> aliases = store.aliases();
        alias = aliases.nextElement();
        if (aliases.hasMoreElements()) throw new Exception("Too many aliases in Keystore, expected exactly one");
        // verify that private key can be loaded, will throw an exception of something is wrong.
        PrivateKey pk = getPrivateKey(alias);
        keyAlgorithm = getAlgorithmOfKey(pk);
        if (logger.isDebugEnabled()) {
            logger.debug("Client Authentication enabled, alias=" + alias);
        }
    }

    private static String getAlgorithmOfKey(PrivateKey pk) {
        if (pk instanceof RSAPrivateKey) {
            return "RSA";
        } else if (pk instanceof DSAPrivateKey) {
            return "DSA";
        } else {
            throw new RuntimeException("Unsupported private key class: " + pk.getClass().getName());
        }
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return new String[]{alias};
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        if (Arrays.asList(keyType).contains(keyAlgorithm)) {
            return alias;
        }
        return null;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return new String[0];
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        try {
            Certificate[] chain = store.getCertificateChain(alias);
            X509Certificate[] x509 = new X509Certificate[chain.length];
            for (int i = 0; i < x509.length; ++i) {
                x509[i] = (X509Certificate) chain[i];
            }
            return x509;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        try {
            return (PrivateKey) store.getKey(alias, decodeBase64(passwordB64).toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
