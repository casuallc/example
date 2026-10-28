package com.apusic.admq.example.jms.consumer;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;

public class SyncConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncConsumer.class);

    private final ClientConfig config;
    private final ConnectionManager connectionManager;

    public SyncConsumer(ClientConfig config, ConnectionManager connectionManager) {
        this.config = config;
        this.connectionManager = connectionManager;
    }

    public void receiveMessages() throws JMSException {
        Session session = connectionManager.getSession();
        Destination destination = connectionManager.createDestination();
        jakarta.jms.MessageConsumer consumer = session.createConsumer(destination);

        int expectedCount = config.getMessageCount();
        int receivedCount = 0;
        long timeout = config.getReceiveTimeout();

        LOGGER.info("Starting synchronous consumer on {}, expecting {} messages, timeout: {} ms",
                config.getDestinationName(), expectedCount, timeout);

        long startTime = System.currentTimeMillis();

        try {
            while (receivedCount < expectedCount) {
                Message message = consumer.receive(timeout);

                if (message == null) {
                    LOGGER.warn("No message received within {} ms timeout. Received {}/{} messages",
                            timeout, receivedCount, expectedCount);
                    break;
                }

                receivedCount++;
                processMessage(message, receivedCount);

                if (config.isTransactionEnabled() && receivedCount % 10 == 0) {
                    connectionManager.commit();
                }
            }

            if (config.isTransactionEnabled() && receivedCount > 0) {
                connectionManager.commit();
            }

        } catch (Exception e) {
            if (config.isTransactionEnabled()) {
                connectionManager.rollback();
            }
            LOGGER.error("Error during message consumption", e);
            throw new JMSException("Consumption failed: " + e.getMessage());
        } finally {
            consumer.close();
        }

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Consumption completed. Received {}/{} messages in {} ms, rate: {} msg/s",
                receivedCount, expectedCount, duration,
                receivedCount * 1000L / Math.max(duration, 1));
    }

    private void processMessage(Message message, int index) throws JMSException {
        String msgIndex = message.getStringProperty("msgIndex");

        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            LOGGER.info("[{}] Received TextMessage: index={}, content={}",
                    index, msgIndex, truncate(textMessage.getText(), 100));
        } else if (message instanceof ObjectMessage) {
            ObjectMessage objectMessage = (ObjectMessage) message;
            LOGGER.info("[{}] Received ObjectMessage: index={}, object={}",
                    index, msgIndex, objectMessage.getObject());
        } else if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            long bodyLength = bytesMessage.getBodyLength();
            LOGGER.info("[{}] Received BytesMessage: index={}, size={} bytes",
                    index, msgIndex, bodyLength);
        } else {
            LOGGER.info("[{}] Received unknown message type: {}", index, message.getClass().getName());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [" + text.length() + " chars total]";
    }
}
