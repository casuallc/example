package com.apusic.admq.example.jms.producer;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;

public class MessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProducer.class);

    private final ClientConfig config;
    private final ConnectionManager connectionManager;

    public MessageProducer(ClientConfig config, ConnectionManager connectionManager) {
        this.config = config;
        this.connectionManager = connectionManager;
    }

    public void sendMessages() throws JMSException {
        Session session = connectionManager.getSession();
        Destination destination = connectionManager.createDestination();
        jakarta.jms.MessageProducer producer = session.createProducer(destination);

        int count = config.getMessageCount();
        LOGGER.info("Starting to send {} messages to {}", count, config.getDestinationName());

        long startTime = System.currentTimeMillis();

        try {
            for (int i = 0; i < count; i++) {
                Message message = createMessage(session, i);
                producer.send(message);

                if (i % 100 == 0 || i == count - 1) {
                    LOGGER.info("Sent message {}/{} to {}", i + 1, count, config.getDestinationName());
                }
            }

            if (config.isTransactionEnabled()) {
                connectionManager.commit();
                LOGGER.info("All {} messages sent and committed", count);
            } else {
                LOGGER.info("All {} messages sent", count);
            }
        } catch (Exception e) {
            if (config.isTransactionEnabled()) {
                connectionManager.rollback();
            }
            throw new JMSException("Failed to send messages: " + e.getMessage());
        } finally {
            producer.close();
        }

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Send completed in {} ms, rate: {} msg/s",
                duration, count * 1000L / Math.max(duration, 1));
    }

    private Message createMessage(Session session, int index) throws JMSException {
        Message message;

        if (config.isTextMessage()) {
            TextMessage textMessage = session.createTextMessage();
            textMessage.setText(config.getMessageContent() + " [" + index + "]");
            message = textMessage;
            LOGGER.debug("Created TextMessage: {}", textMessage.getText());
        } else if (config.isObjectMessage()) {
            ObjectMessage objectMessage = session.createObjectMessage();
            objectMessage.setObject(config.getMessageContent() + " [" + index + "]");
            message = objectMessage;
            LOGGER.debug("Created ObjectMessage");
        } else if (config.isBytesMessage()) {
            BytesMessage bytesMessage = session.createBytesMessage();
            byte[] data = createMessageData(index);
            bytesMessage.writeBytes(data);
            message = bytesMessage;
            LOGGER.debug("Created BytesMessage, size: {} bytes", data.length);
        } else {
            throw new IllegalArgumentException("Unknown message type: " + config.getMessageType());
        }

        message.setIntProperty("msgIndex", index);

        if (config.hasMessageGroupId()) {
            message.setStringProperty("JMSXGroupID", config.getMessageGroupId());
            LOGGER.debug("Set JMSXGroupID: {}", config.getMessageGroupId());
        }

        return message;
    }

    private byte[] createMessageData(int index) {
        int size = config.getMessageSize();
        String prefix = config.getMessageContent() + " [" + index + "]";
        byte[] prefixBytes = prefix.getBytes();

        if (size <= prefixBytes.length) {
            return prefixBytes;
        }

        byte[] data = new byte[size];
        System.arraycopy(prefixBytes, 0, data, 0, prefixBytes.length);

        for (int i = prefixBytes.length; i < size; i++) {
            data[i] = (byte) ('a' + (i % 26));
        }

        return data;
    }
}
