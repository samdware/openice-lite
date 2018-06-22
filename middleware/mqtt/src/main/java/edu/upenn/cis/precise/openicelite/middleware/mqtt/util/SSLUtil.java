package edu.upenn.cis.precise.openicelite.middleware.mqtt.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.*;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;

/**
 * Helper to build SSL Socket Factory for MQTT TLS communication
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public class SSLUtil {
    private static final Logger logger = LogManager.getLogger(SSLUtil.class);

    public static SSLSocketFactory getSocketFactory(String caCertFile, String clientCertFile,
                                                    String clientKeyFile, String keyPassword)
            throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        if (caCertFile == null || caCertFile.isEmpty()) throw new IllegalArgumentException("Invalid CA cert file");
        boolean enableClientCert = clientCertFile != null && !clientCertFile.isEmpty() &&
                clientKeyFile != null && !clientKeyFile.isEmpty();
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing SSLSocket - CA Cert: " + caCertFile +
                    " -- Client Cert: " + (clientCertFile == null ? "null" : clientCertFile ) +
                    " -- Client Key: " + (clientKeyFile == null ? "null" : clientKeyFile ));
        }

        // Prepare converters
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        certConverter.setProvider("BC");
        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

        // Load CA certificate
        PEMParser parser = new PEMParser(new InputStreamReader(classloader.getResourceAsStream(caCertFile)));
        X509CertificateHolder caCertHolder = (X509CertificateHolder) parser.readObject();
        X509Certificate caCert = certConverter.getCertificate(caCertHolder);
        parser.close();

        // Create Key Store for authenticating server
        KeyStore trustedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustedKeyStore.load(null, null);
        trustedKeyStore.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustedKeyStore);

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        if (enableClientCert) {
            // Load Client certificate
            parser = new PEMParser(new InputStreamReader(classloader.getResourceAsStream(clientCertFile)));
            X509CertificateHolder clientCertHolder = (X509CertificateHolder) parser.readObject();
            X509Certificate clientCert = certConverter.getCertificate(clientCertHolder);
            parser.close();

            // Load Client key
            parser = new PEMParser(new InputStreamReader(classloader.getResourceAsStream(clientKeyFile)));
            Object keyPair = parser.readObject();
            PrivateKey privateKey;
            if (keyPassword != null && !keyPassword.isEmpty()) {
                PEMDecryptorProvider decrypter = new JcePEMDecryptorProviderBuilder().build(keyPassword.toCharArray());
                PEMKeyPair decryptedKeyPair = ((PEMEncryptedKeyPair) keyPair).decryptKeyPair(decrypter);
                PrivateKeyInfo keyInfo = decryptedKeyPair.getPrivateKeyInfo();
                privateKey = keyConverter.getPrivateKey(keyInfo);
            } else {
                privateKey = keyConverter.getPrivateKey(((PEMKeyPair) keyPair).getPrivateKeyInfo());
            }
            parser.close();

            // Create Key Store for client
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("certificate", clientCert);
            if (keyPassword != null && !keyPassword.isEmpty()) {
                keyStore.setKeyEntry("private-key", privateKey, keyPassword.toCharArray(),
                        new java.security.cert.Certificate[]{clientCert});
                kmf.init(keyStore, keyPassword.toCharArray());
            } else {
                keyStore.setKeyEntry("private-key", privateKey.getEncoded(),
                        new java.security.cert.Certificate[]{clientCert});
                kmf.init(keyStore, null);
            }
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return context.getSocketFactory();
        } else {
            context.init(null, tmf.getTrustManagers(), null);
            return context.getSocketFactory();
        }
    }
}
