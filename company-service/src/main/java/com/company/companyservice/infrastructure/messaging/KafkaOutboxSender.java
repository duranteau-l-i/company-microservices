package com.company.companyservice.infrastructure.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaOutboxSender implements OutboxKafkaSender {

    private final KafkaTemplate<String, String> outboxKafkaTemplate;

    public KafkaOutboxSender(KafkaTemplate<String, String> outboxKafkaTemplate) {
        this.outboxKafkaTemplate = outboxKafkaTemplate;
    }

    @Override
    public void send(String topic, String key, String payload) throws Exception {
        outboxKafkaTemplate.send(topic, key, payload).get(10, TimeUnit.SECONDS);
    }
}
