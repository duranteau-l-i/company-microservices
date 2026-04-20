package com.company.userservice.infrastructure.adapter.out.messaging;

import com.company.userservice.domain.event.DomainEvent;
import com.company.userservice.domain.port.out.UserEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaUserEventPublisher implements UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaUserEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.user-events:user-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        EventEnvelope envelope = EventEnvelope.wrap(event);
        kafkaTemplate.send(topic, event.aggregateId().toString(), envelope);
    }
}
