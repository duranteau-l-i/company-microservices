package com.company.userservice.unit.infrastructure;

import com.company.userservice.infrastructure.messaging.OutboxRelayService;
import com.company.userservice.infrastructure.persistence.command.OutboxEventEntity;
import com.company.userservice.stubs.InMemoryOutboxEventEntityRepository;
import com.company.userservice.stubs.InMemoryOutboxKafkaSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayServiceTest {

    private InMemoryOutboxEventEntityRepository outboxRepo;
    private InMemoryOutboxKafkaSender kafkaSender;
    private OutboxRelayService relayService;

    @BeforeEach
    void setUp() {
        outboxRepo = new InMemoryOutboxEventEntityRepository();
        kafkaSender = new InMemoryOutboxKafkaSender();
        relayService = new OutboxRelayService(outboxRepo, kafkaSender);
    }

    @Test
    void processOne_sendsMessageAndMarksPublished() throws Exception {
        OutboxEventEntity pending = pendingEntry();
        outboxRepo.save(pending);

        relayService.processOne(pending.getId());

        assertThat(kafkaSender.sentMessages()).hasSize(1);
        InMemoryOutboxKafkaSender.SentMessage sent = kafkaSender.sentMessages().get(0);
        assertThat(sent.topic()).isEqualTo("user-events");
        assertThat(sent.key()).isEqualTo(pending.getAggregateId().toString());
        assertThat(sent.payload()).isEqualTo(pending.getPayload());

        OutboxEventEntity updated = outboxRepo.findById(pending.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("PUBLISHED");
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @Test
    void processOne_skipsMissingEntry() {
        relayService.processOne(UUID.randomUUID());

        assertThat(kafkaSender.sentMessages()).isEmpty();
    }

    @Test
    void processOne_leavesEntryPendingOnKafkaFailure() {
        InMemoryOutboxKafkaSender failingSender = new InMemoryOutboxKafkaSender() {
            @Override
            public void send(String topic, String key, String payload) throws Exception {
                throw new RuntimeException("broker down");
            }
        };
        relayService = new OutboxRelayService(outboxRepo, failingSender);

        OutboxEventEntity pending = pendingEntry();
        outboxRepo.save(pending);

        relayService.processOne(pending.getId());

        OutboxEventEntity unchanged = outboxRepo.findById(pending.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo("PENDING");
        assertThat(unchanged.getPublishedAt()).isNull();
    }

    @Test
    void processOne_skipsAlreadyPublishedEntry() {
        OutboxEventEntity published = new OutboxEventEntity(
                UUID.randomUUID(), UUID.randomUUID(), "UserCreatedEvent",
                "user-events", "{}", "PUBLISHED", Instant.now(), Instant.now());
        outboxRepo.save(published);

        relayService.processOne(published.getId());

        assertThat(kafkaSender.sentMessages()).isEmpty();
    }

    private OutboxEventEntity pendingEntry() {
        return new OutboxEventEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "UserCreatedEvent",
                "user-events",
                "{\"eventId\":\"abc\",\"eventType\":\"UserCreatedEvent\"}",
                "PENDING",
                Instant.now(),
                null
        );
    }
}
