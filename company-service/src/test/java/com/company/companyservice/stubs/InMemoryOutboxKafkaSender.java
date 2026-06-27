package com.company.companyservice.stubs;

import com.company.companyservice.infrastructure.messaging.OutboxKafkaSender;

import java.util.ArrayList;
import java.util.List;

public class InMemoryOutboxKafkaSender implements OutboxKafkaSender {

    private final List<SentMessage> sent = new ArrayList<>();

    @Override
    public void send(String topic, String key, String payload) throws Exception {
        sent.add(new SentMessage(topic, key, payload));
    }

    public List<SentMessage> sentMessages() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }

    public record SentMessage(String topic, String key, String payload) {}
}
