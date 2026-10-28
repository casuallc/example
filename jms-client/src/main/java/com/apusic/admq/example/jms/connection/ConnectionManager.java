package com.apusic.admq.example.jms.connection;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.jms.AdmqConnectionFactory;
import com.apusic.admq.jms.config.AdmqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;

public class ConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private final ClientConfig config;
    private Connection connection;
    private Session session;

    public ConnectionManager(ClientConfig config) {
        this.config = config;
    }

    public void start() throws JMSException {
        LOGGER.info("Connecting to broker: {}", config.getBrokerUrl());

        AdmqConnectionFactory factory = createFactory();

        if (config.hasAuthentication()) {
            LOGGER.info("Connecting with authentication, username: {}", config.getUsername());
            this.connection = factory.createConnection(config.getUsername(), config.getPassword());
        } else {
            this.connection = factory.createConnection();
        }

        this.connection.start();

        boolean transacted = config.isTransactionEnabled();
        int acknowledgeMode = transacted ? Session.SESSION_TRANSACTED : Session.AUTO_ACKNOWLEDGE;
        this.session = connection.createSession(transacted, acknowledgeMode);

        LOGGER.info("Connection established. Transacted: {}, AckMode: {}",
                transacted, acknowledgeMode);
    }

    private AdmqConnectionFactory createFactory() throws JMSException {
        if (config.isTls()) {
            try {
                TlsSupport.applyDefault(config);
            } catch (Exception e) {
                JMSException jmsException = new JMSException("Failed to initialise TLS: " + e.getMessage());
                jmsException.initCause(e);
                throw jmsException;
            }
            return new AdmqConnectionFactory(config.getBrokerUrl());
        }

        if (!config.isGmtls()) {
            return new AdmqConnectionFactory(config.getBrokerUrl());
        }

        LOGGER.info("GMTLS (TLCP) enabled, signCert={}, encCert={}, caCert={}",
                config.getGmtlsSignCert(), config.getGmtlsEncCert(), config.getGmtlsCaCert());

        AdmqConfig admqConfig = new AdmqConfig()
                .brokerURL(config.getBrokerUrl())
                .signCert(config.getGmtlsSignCert())
                .signKey(config.getGmtlsSignKey())
                .encCert(config.getGmtlsEncCert())
                .encKey(config.getGmtlsEncKey())
                .caCert(config.getGmtlsCaCert());

        if (!config.getGmtlsKeyPassword().isEmpty()) {
            admqConfig.keyPassword(config.getGmtlsKeyPassword());
        }
        if (!config.getGmtlsCipherSuites().isEmpty()) {
            admqConfig.cipherSuites(config.getGmtlsCipherSuites());
        }

        return new AdmqConnectionFactory(admqConfig);
    }

    public Session getSession() {
        return session;
    }

    public Destination createDestination() throws JMSException {
        String name = config.getDestinationName();
        if (config.isQueue()) {
            LOGGER.info("Creating Queue destination: {}", name);
            return session.createQueue(name);
        } else {
            LOGGER.info("Creating Topic destination: {}", name);
            return session.createTopic(name);
        }
    }

    public void commit() throws JMSException {
        if (session != null && config.isTransactionEnabled()) {
            session.commit();
            LOGGER.info("Transaction committed");
        }
    }

    public void rollback() throws JMSException {
        if (session != null && config.isTransactionEnabled()) {
            session.rollback();
            LOGGER.warn("Transaction rolled back");
        }
    }

    public void close() {
        LOGGER.info("Closing connection resources...");

        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                LOGGER.warn("Error closing session", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                LOGGER.warn("Error closing connection", e);
            }
        }

        LOGGER.info("Connection resources closed");
    }
}
