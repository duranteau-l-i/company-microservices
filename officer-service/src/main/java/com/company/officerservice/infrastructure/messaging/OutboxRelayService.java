package com.company.officerservice.infrastructure.messaging;

import com.company.officerservice.infrastructure.persistence.command.OutboxRelayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxRelayRepository outboxRepository;
    private final OutboxKafkaSender kafkaSender;

    public OutboxRelayService(OutboxRelayRepository outboxRepository, OutboxKafkaSender kafkaSender) {
        this.outboxRepository = outboxRepository;
        this.kafkaSender = kafkaSender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID outboxId) {
        outboxRepository.findPendingByIdWithLock(outboxId).ifPresent(entry -> {
            try {
                kafkaSender.send(entry.getTopic(), entry.getAggregateId().toString(), entry.getPayload());
                entry.markPublished(Instant.now());
                outboxRepository.save(entry);
            } catch (Exception e) {
                log.error("Relay failed for outbox entry {}, leaving PENDING for retry", outboxId, e);
            }
        });
    }
}
