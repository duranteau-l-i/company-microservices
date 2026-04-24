package com.company.companyservice.infrastructure.messaging;

import com.company.companyservice.domain.event.DomainEvent;
import com.company.companyservice.domain.port.infrastructure.CompanyEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements CompanyEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.company-events:company-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        EventEnvelope envelope = EventEnvelope.wrap(event);
        kafkaTemplate.send(topic, event.aggregateId().toString(), envelope);
    }
}
