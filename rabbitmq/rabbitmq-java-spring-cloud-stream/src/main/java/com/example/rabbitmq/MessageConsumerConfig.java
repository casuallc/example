package com.example.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class MessageConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumerConfig.class);

    @Bean
    public Consumer<String> demoConsumer() {
        return message -> log.info("Received message: {}", message);
    }
}
