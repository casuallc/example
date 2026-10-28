package com.example.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${demo.exchange}")
    private String exchange;

    @Value("${demo.queue}")
    private String queue;

    @Value("${demo.routing-key}")
    private String routingKey;

    @Bean
    public TopicExchange demoExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue demoQueue() {
        return new Queue(queue, true);
    }

    @Bean
    public Binding demoBinding(Queue demoQueue, TopicExchange demoExchange) {
        return BindingBuilder.bind(demoQueue).to(demoExchange).with(routingKey);
    }
}
