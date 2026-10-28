package com.apusic.admq.example.jms.connection;

import com.apusic.admq.example.jms.config.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * 依据 {@link ClientConfig} 的 tls.* 配置构建标准 TLS (ssl://) {@link SSLContext}。
 *
 * <p>底层 ActiveMQ 的 ssl 传输在未显式设置时回退到 JVM 默认 SSLContext，
 * 因此 JMS 连接前通过 {@link #applyDefault(ClientConfig)} 设置默认上下文；
 * 该方式与线程无关，failover 重连同样生效。
 */
public final class TlsSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSupport.class);

    private TlsSupport() {
    }

    /**
     * 按配置构建 TLS {@link SSLContext}（不设为 JVM 默认，供探测/验证用）。
     *
     * @param config             客户端配置
     * @param withClientCert     是否加载客户端证书（mTLS）
     * @param trustStoreOverride 覆盖 truststore 路径（如负向用例的"错误 CA"），null 用配置值
     */
    public static SSLContext build(ClientConfig config, boolean withClientCert,
                                   String trustStoreOverride) throws Exception {
        String trustStorePath = trustStoreOverride != null ? trustStoreOverride : config.getTlsTrustStore();
        KeyStore trustStore = KeyStore.getInstance(config.getTlsTrustStoreType());
        try (InputStream in = new FileInputStream(trustStorePath)) {
            trustStore.load(in, config.getTlsTrustStorePassword().toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        KeyManager[] keyManagers = null;
        if (withClientCert) {
            KeyStore keyStore = KeyStore.getInstance(config.getTlsKeyStoreType());
            try (InputStream in = new FileInputStream(config.getTlsKeyStore())) {
                keyStore.load(in, config.getTlsKeyStorePassword().toCharArray());
            }
            String keyPassword = config.getTlsKeyPassword().isEmpty()
                    ? config.getTlsKeyStorePassword() : config.getTlsKeyPassword();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyPassword.toCharArray());
            keyManagers = kmf.getKeyManagers();
        }

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, tmf.getTrustManagers(), new SecureRandom());
        return context;
    }

    /**
     * 按配置构建 TLS {@link SSLContext} 并设为 JVM 默认（配置了 tls.key.store 时
     * 携带客户端证书，用于 mTLS）。供 JMS ssl:// 连接使用。
     */
    public static void applyDefault(ClientConfig config) throws Exception {
        if (config.getTlsTrustStore().isEmpty()) {
            LOGGER.warn("tls.trust.store not set, ssl:// will use JVM default truststore (cacerts)");
            return;
        }
        boolean withClientCert = config.hasTlsKeyStore();
        SSLContext context = build(config, withClientCert, null);
        SSLContext.setDefault(context);
        LOGGER.info("TLS default SSLContext applied, trustStore={}, clientCert={}",
                config.getTlsTrustStore(), withClientCert ? config.getTlsKeyStore() : "(none)");
    }
}
