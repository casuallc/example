package com.example.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);

    private final StreamBridge streamBridge;

    @Value("${demo.exchange}")
    private String exchange;

    @Value("${demo.routing-key}")
    private String routingKey;

    public MessageProducer(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void send(String message) {
        streamBridge.send("demoProducer-out-0", message);
        log.info("Sent message to exchange={} routingKey={}: {}", exchange, routingKey, message);
    }
}
