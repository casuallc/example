package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import com.apusic.admq.example.jms.consumer.AsyncConsumer;
import com.apusic.admq.example.jms.consumer.SyncConsumer;
import com.apusic.admq.example.jms.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTestScenario implements TestScenario {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestScenario.class);

    @Override
    public void execute(ClientConfig config) throws Exception {
        ConnectionManager connectionManager = new ConnectionManager(config);

        try {
            connectionManager.start();
            doExecute(config, connectionManager);
        } finally {
            connectionManager.close();
        }
    }

    protected abstract void doExecute(ClientConfig config, ConnectionManager connectionManager) throws Exception;

    protected void sendOnly(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        MessageProducer producer = new MessageProducer(config, connectionManager);
        producer.sendMessages();
    }

    protected void receiveOnly(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        if (config.isSyncConsumer()) {
            SyncConsumer consumer = new SyncConsumer(config, connectionManager);
            consumer.receiveMessages();
        } else {
            AsyncConsumer consumer = new AsyncConsumer(config, connectionManager);
            consumer.startListening();
            long timeout = config.getMessageCount() * config.getReceiveTimeout() + 10000;
            consumer.awaitCompletion(timeout);
        }
    }

    protected void sendAndReceive(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        AsyncConsumer consumer = new AsyncConsumer(config, connectionManager);
        consumer.startListening();

        Thread.sleep(500);

        MessageProducer producer = new MessageProducer(config, connectionManager);
        producer.sendMessages();

        long timeout = config.getMessageCount() * config.getReceiveTimeout() + 10000;
        boolean completed = consumer.awaitCompletion(timeout);

        if (completed) {
            LOGGER.info("End-to-end test passed: all messages sent and received");
        } else {
            LOGGER.warn("End-to-end test may have issues: not all messages were received in time");
        }
    }
}
