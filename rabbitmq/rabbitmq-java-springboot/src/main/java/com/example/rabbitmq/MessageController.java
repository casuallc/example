package com.example.rabbitmq;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageProducer producer;

    public MessageController(MessageProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> send(@RequestBody(required = false) Map<String, String> body) {
        String message = body == null ? "hello from springboot" : body.getOrDefault("message", "hello from springboot");
        producer.send(message);
        return ResponseEntity.ok(Map.of("status", "sent", "message", message));
    }
}
