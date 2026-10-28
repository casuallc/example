package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderedMessageTestScenario extends AbstractTestScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderedMessageTestScenario.class);

    @Override
    protected void doExecute(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        LOGGER.info("Executing Ordered Message test scenario");

        if (!config.hasMessageGroupId()) {
            LOGGER.warn("Ordered message test scenario is running but message.group.id is not set. " +
                    "Messages may not be processed in order.");
        } else {
            LOGGER.info("Using JMSXGroupID: {}", config.getMessageGroupId());
        }

        if (config.isSendOnly()) {
            sendOnly(config, connectionManager);
        } else if (config.isReceiveOnly()) {
            receiveOnly(config, connectionManager);
        } else {
            sendAndReceive(config, connectionManager);
        }

        LOGGER.info("Ordered Message test scenario completed");
    }
}
