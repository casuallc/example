package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicTestScenario extends AbstractTestScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTestScenario.class);

    @Override
    protected void doExecute(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        LOGGER.info("Executing Topic test scenario");

        if (config.isSendOnly()) {
            sendOnly(config, connectionManager);
        } else if (config.isReceiveOnly()) {
            receiveOnly(config, connectionManager);
        } else {
            LOGGER.warn("Topic BOTH mode requires consumer to be started before producer. " +
                    "For reliable testing, use separate SEND_ONLY and RECEIVE_ONLY processes.");
            sendAndReceive(config, connectionManager);
        }

        LOGGER.info("Topic test scenario completed");
    }
}
