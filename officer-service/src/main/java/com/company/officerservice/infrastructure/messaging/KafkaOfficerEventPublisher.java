package com.company.officerservice.infrastructure.messaging;

import com.company.officerservice.domain.event.DomainEvent;
import com.company.officerservice.domain.port.infrastructure.OfficerEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOfficerEventPublisher implements OfficerEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaOfficerEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.officer-events:officer-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        EventEnvelope envelope = EventEnvelope.wrap(event);
        kafkaTemplate.send(topic, event.aggregateId().toString(), envelope);
    }
}
