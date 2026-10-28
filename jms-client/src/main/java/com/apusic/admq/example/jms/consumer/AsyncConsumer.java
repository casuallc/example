package com.apusic.admq.example.jms.consumer;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncConsumer.class);

    private final ClientConfig config;
    private final ConnectionManager connectionManager;
    private final AtomicInteger receivedCount = new AtomicInteger(0);
    private CountDownLatch latch;

    public AsyncConsumer(ClientConfig config, ConnectionManager connectionManager) {
        this.config = config;
        this.connectionManager = connectionManager;
    }

    public void startListening() throws JMSException {
        int expectedCount = config.getMessageCount();
        this.latch = new CountDownLatch(expectedCount);
        receivedCount.set(0);

        Session session = connectionManager.getSession();
        Destination destination = connectionManager.createDestination();
        jakarta.jms.MessageConsumer consumer = session.createConsumer(destination);

        LOGGER.info("Starting asynchronous consumer on {}, expecting {} messages",
                config.getDestinationName(), expectedCount);

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    int count = receivedCount.incrementAndGet();
                    processMessage(message, count);
                    latch.countDown();

                    if (config.isTransactionEnabled() && count % 10 == 0) {
                        connectionManager.commit();
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing message", e);
                    if (config.isTransactionEnabled()) {
                        try {
                            connectionManager.rollback();
                        } catch (JMSException ex) {
                            LOGGER.error("Failed to rollback transaction", ex);
                        }
                    }
                }
            }
        });
    }

    public boolean awaitCompletion(long timeoutMs) {
        try {
            boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (completed) {
                LOGGER.info("All {} messages received", receivedCount.get());
                if (config.isTransactionEnabled()) {
                    try {
                        connectionManager.commit();
                    } catch (JMSException e) {
                        LOGGER.error("Failed to commit transaction", e);
                    }
                }
            } else {
                LOGGER.warn("Timeout after {} ms. Received {}/{} messages",
                        timeoutMs, receivedCount.get(), config.getMessageCount());
            }
            return completed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while waiting for messages", e);
            return false;
        }
    }

    private void processMessage(Message message, int index) throws JMSException {
        String msgIndex = message.getStringProperty("msgIndex");

        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            LOGGER.info("[{}] Async received TextMessage: index={}, content={}",
                    index, msgIndex, truncate(textMessage.getText(), 100));
        } else if (message instanceof ObjectMessage) {
            ObjectMessage objectMessage = (ObjectMessage) message;
            LOGGER.info("[{}] Async received ObjectMessage: index={}, object={}",
                    index, msgIndex, objectMessage.getObject());
        } else if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            long bodyLength = bytesMessage.getBodyLength();
            LOGGER.info("[{}] Async received BytesMessage: index={}, size={} bytes",
                    index, msgIndex, bodyLength);
        } else {
            LOGGER.info("[{}] Async received unknown message type: {}",
                    index, message.getClass().getName());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [" + text.length() + " chars total]";
    }
}
