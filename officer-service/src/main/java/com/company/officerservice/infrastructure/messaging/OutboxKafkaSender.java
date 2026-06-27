package com.company.officerservice.infrastructure.messaging;

public interface OutboxKafkaSender {
    void send(String topic, String key, String payload) throws Exception;
}
