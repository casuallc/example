package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTestScenario extends AbstractTestScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionTestScenario.class);

    @Override
    protected void doExecute(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        if (!config.isTransactionEnabled()) {
            LOGGER.warn("Transaction test scenario is running but transaction.enabled is false in config. " +
                    "Transaction behavior may not be verified correctly.");
        }

        LOGGER.info("Executing Transaction test scenario");

        if (config.isSendOnly()) {
            sendOnly(config, connectionManager);
        } else if (config.isReceiveOnly()) {
            receiveOnly(config, connectionManager);
        } else {
            sendAndReceive(config, connectionManager);
        }

        LOGGER.info("Transaction test scenario completed");
    }
}
