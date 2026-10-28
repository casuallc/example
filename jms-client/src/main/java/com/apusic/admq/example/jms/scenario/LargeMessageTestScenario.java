package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LargeMessageTestScenario extends AbstractTestScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(LargeMessageTestScenario.class);

    @Override
    protected void doExecute(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        LOGGER.info("Executing Large Message test scenario, message size: {} bytes", config.getMessageSize());

        if (!config.isBytesMessage()) {
            LOGGER.warn("Large message test is most effective with BYTES message type. " +
                    "Current type: {}", config.getMessageType());
        }

        if (config.isSendOnly()) {
            sendOnly(config, connectionManager);
        } else if (config.isReceiveOnly()) {
            receiveOnly(config, connectionManager);
        } else {
            sendAndReceive(config, connectionManager);
        }

        LOGGER.info("Large Message test scenario completed");
    }
}
