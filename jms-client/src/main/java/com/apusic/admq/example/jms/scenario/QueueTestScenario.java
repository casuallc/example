package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueTestScenario extends AbstractTestScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueTestScenario.class);

    @Override
    protected void doExecute(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        LOGGER.info("Executing Queue test scenario");

        if (config.isSendOnly()) {
            sendOnly(config, connectionManager);
        } else if (config.isReceiveOnly()) {
            receiveOnly(config, connectionManager);
        } else {
            sendAndReceive(config, connectionManager);
        }

        LOGGER.info("Queue test scenario completed");
    }
}
