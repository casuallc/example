package com.apusic.admq.example.jms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfig.class);

    private final String brokerUrl;
    private final String scenarioType;
    private final String runMode;
    private final String destinationName;
    private final String destinationType;
    private final String messageType;
    private final int messageCount;
    private final String messageContent;
    private final int messageSize;
    private final String messageGroupId;
    private final String consumerMode;
    private final int consumerThreads;
    private final boolean transactionEnabled;
    private final long receiveTimeout;
    private final String username;
    private final String password;

    // GMTLS (TLCP) 传输加密配置，仅当 broker.url 使用 gmtls:// 时生效
    private final String gmtlsSignCert;
    private final String gmtlsSignKey;
    private final String gmtlsEncCert;
    private final String gmtlsEncKey;
    private final String gmtlsCaCert;
    private final String gmtlsKeyPassword;
    private final String gmtlsCipherSuites;

    // TLS (ssl://) 传输加密配置，仅当 broker.url 使用 ssl:// 时生效
    private final String tlsTrustStore;
    private final String tlsTrustStorePassword;
    private final String tlsTrustStoreType;
    private final String tlsKeyStore;
    private final String tlsKeyStorePassword;
    private final String tlsKeyStoreType;
    private final String tlsKeyPassword;
    private final String tlsWrongTrustStore;
    private final boolean tlsClientAuthRequired;

    public ClientConfig(String configFilePath) {
        Properties props = loadProperties(configFilePath);

        this.brokerUrl = getString(props, "broker.url", "tcp://localhost:61616");
        this.scenarioType = getString(props, "scenario.type", "QUEUE");
        this.runMode = getString(props, "run.mode", "SEND_ONLY");
        this.destinationName = getString(props, "destination.name", "test.queue");
        this.destinationType = getString(props, "destination.type", "QUEUE");
        this.messageType = getString(props, "message.type", "TEXT");
        this.messageCount = getInt(props, "message.count", 1);
        this.messageContent = getString(props, "message.content", "Hello admq");
        this.messageSize = getInt(props, "message.size", 1024);
        this.messageGroupId = getString(props, "message.group.id", "");
        this.consumerMode = getString(props, "consumer.mode", "SYNC");
        this.consumerThreads = getInt(props, "consumer.threads", 1);
        this.transactionEnabled = getBoolean(props, "transaction.enabled", false);
        this.receiveTimeout = getLong(props, "receive.timeout", 5000);
        this.username = getString(props, "broker.username", "");
        this.password = getString(props, "broker.password", "");
        this.gmtlsSignCert = getString(props, "gmtls.sign.cert", "");
        this.gmtlsSignKey = getString(props, "gmtls.sign.key", "");
        this.gmtlsEncCert = getString(props, "gmtls.enc.cert", "");
        this.gmtlsEncKey = getString(props, "gmtls.enc.key", "");
        this.gmtlsCaCert = getString(props, "gmtls.ca.cert", "");
        this.gmtlsKeyPassword = getString(props, "gmtls.key.password", "");
        this.gmtlsCipherSuites = getString(props, "gmtls.cipher.suites", "");
        this.tlsTrustStore = getString(props, "tls.trust.store", "");
        this.tlsTrustStorePassword = getString(props, "tls.trust.store.password", "");
        this.tlsTrustStoreType = getString(props, "tls.trust.store.type", "PKCS12");
        this.tlsKeyStore = getString(props, "tls.key.store", "");
        this.tlsKeyStorePassword = getString(props, "tls.key.store.password", "");
        this.tlsKeyStoreType = getString(props, "tls.key.store.type", "PKCS12");
        this.tlsKeyPassword = getString(props, "tls.key.password", "");
        this.tlsWrongTrustStore = getString(props, "tls.wrong.trust.store", "");
        this.tlsClientAuthRequired = getBoolean(props, "tls.client.auth.required", false);

        LOGGER.info("Loaded config from: {}", configFilePath);
        LOGGER.info("Broker URL: {}, Scenario: {}, Run Mode: {}", brokerUrl, scenarioType, runMode);
    }

    private Properties loadProperties(String configFilePath) {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(configFilePath)) {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + configFilePath, e);
        }
        return props;
    }

    private String getString(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
        return value != null ? value.trim() : defaultValue;
    }

    private int getInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid integer value for {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private long getLong(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid long value for {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private boolean getBoolean(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getScenarioType() {
        return scenarioType;
    }

    public String getRunMode() {
        return runMode;
    }

    public boolean isSendOnly() {
        return "SEND_ONLY".equalsIgnoreCase(runMode);
    }

    public boolean isReceiveOnly() {
        return "RECEIVE_ONLY".equalsIgnoreCase(runMode);
    }

    public boolean isBothMode() {
        return "BOTH".equalsIgnoreCase(runMode);
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public boolean isQueue() {
        return "QUEUE".equalsIgnoreCase(destinationType);
    }

    public boolean isTopic() {
        return "TOPIC".equalsIgnoreCase(destinationType);
    }

    public String getMessageType() {
        return messageType;
    }

    public boolean isTextMessage() {
        return "TEXT".equalsIgnoreCase(messageType);
    }

    public boolean isObjectMessage() {
        return "OBJECT".equalsIgnoreCase(messageType);
    }

    public boolean isBytesMessage() {
        return "BYTES".equalsIgnoreCase(messageType);
    }

    public int getMessageCount() {
        return messageCount;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public String getMessageGroupId() {
        return messageGroupId;
    }

    public boolean hasMessageGroupId() {
        return messageGroupId != null && !messageGroupId.isEmpty();
    }

    public String getConsumerMode() {
        return consumerMode;
    }

    public boolean isSyncConsumer() {
        return "SYNC".equalsIgnoreCase(consumerMode);
    }

    public boolean isAsyncConsumer() {
        return "ASYNC".equalsIgnoreCase(consumerMode);
    }

    public int getConsumerThreads() {
        return consumerThreads;
    }

    public boolean isTransactionEnabled() {
        return transactionEnabled;
    }

    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean hasAuthentication() {
        return username != null && !username.isEmpty();
    }

    /**
     * 是否启用 GMTLS (TLCP) 传输加密：broker.url 使用 gmtls:// scheme 时返回 true
     * （含 failover:(gmtls://...) 形式）。
     */
    public boolean isGmtls() {
        return brokerUrl != null && brokerUrl.contains("gmtls://");
    }

    public String getGmtlsSignCert() {
        return gmtlsSignCert;
    }

    public String getGmtlsSignKey() {
        return gmtlsSignKey;
    }

    public String getGmtlsEncCert() {
        return gmtlsEncCert;
    }

    public String getGmtlsEncKey() {
        return gmtlsEncKey;
    }

    public String getGmtlsCaCert() {
        return gmtlsCaCert;
    }

    public String getGmtlsKeyPassword() {
        return gmtlsKeyPassword;
    }

    public String getGmtlsCipherSuites() {
        return gmtlsCipherSuites;
    }

    /**
     * 是否使用标准 TLS 传输加密：broker.url 使用 ssl:// scheme 时返回 true
     * （含 failover:(ssl://...) 形式）。
     */
    public boolean isTls() {
        return brokerUrl != null && brokerUrl.contains("ssl://");
    }

    public String getTlsTrustStore() {
        return tlsTrustStore;
    }

    public String getTlsTrustStorePassword() {
        return tlsTrustStorePassword;
    }

    public String getTlsTrustStoreType() {
        return tlsTrustStoreType;
    }

    public String getTlsKeyStore() {
        return tlsKeyStore;
    }

    public String getTlsKeyStorePassword() {
        return tlsKeyStorePassword;
    }

    public String getTlsKeyStoreType() {
        return tlsKeyStoreType;
    }

    public String getTlsKeyPassword() {
        return tlsKeyPassword;
    }

    public String getTlsWrongTrustStore() {
        return tlsWrongTrustStore;
    }

    public boolean isTlsClientAuthRequired() {
        return tlsClientAuthRequired;
    }

    public boolean hasTlsKeyStore() {
        return tlsKeyStore != null && !tlsKeyStore.isEmpty();
    }

    @Override
    public String toString() {
        return "ClientConfig{" +
                "brokerUrl='" + brokerUrl + '\'' +
                ", scenarioType='" + scenarioType + '\'' +
                ", runMode='" + runMode + '\'' +
                ", destinationName='" + destinationName + '\'' +
                ", destinationType='" + destinationType + '\'' +
                ", messageType='" + messageType + '\'' +
                ", messageCount=" + messageCount +
                ", messageSize=" + messageSize +
                ", transactionEnabled=" + transactionEnabled +
                '}';
    }
}
