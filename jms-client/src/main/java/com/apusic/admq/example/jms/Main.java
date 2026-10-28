package com.apusic.admq.example.jms;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.scenario.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String configFilePath = args.length > 0 ? args[0] : "config/queue-send.properties";

        LOGGER.info("============================================");
        LOGGER.info("admq Test Client Starting");
        LOGGER.info("Config file: {}", configFilePath);
        LOGGER.info("============================================");

        try {
            ClientConfig config = new ClientConfig(configFilePath);
            LOGGER.info("Configuration loaded: {}", config);

            TestScenario scenario = createScenario(config.getScenarioType());
            LOGGER.info("Executing scenario: {}", config.getScenarioType());

            long startTime = System.currentTimeMillis();
            scenario.execute(config);
            long duration = System.currentTimeMillis() - startTime;

            LOGGER.info("============================================");
            LOGGER.info("Scenario completed in {} ms", duration);
            LOGGER.info("============================================");

        } catch (Exception e) {
            LOGGER.error("Test execution failed", e);
            System.exit(1);
        }
    }

    private static TestScenario createScenario(String scenarioType) {
        switch (scenarioType.toUpperCase()) {
            case "QUEUE":
                return new QueueTestScenario();
            case "TOPIC":
                return new TopicTestScenario();
            case "TRANSACTION":
                return new TransactionTestScenario();
            case "LARGE":
            case "LARGE_MESSAGE":
                return new LargeMessageTestScenario();
            case "ORDERED":
            case "ORDERED_MESSAGE":
                return new OrderedMessageTestScenario();
            case "GMTLS":
                return new GmtlsTestScenario();
            case "TLS":
                return new TlsTestScenario();
            default:
                throw new IllegalArgumentException("Unknown scenario type: " + scenarioType +
                        ". Supported: QUEUE, TOPIC, TRANSACTION, LARGE, ORDERED, GMTLS, TLS");
        }
    }
}
