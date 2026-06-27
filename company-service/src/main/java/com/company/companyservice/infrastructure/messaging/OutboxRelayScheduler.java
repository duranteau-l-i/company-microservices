package com.company.companyservice.infrastructure.messaging;

import com.company.companyservice.infrastructure.persistence.command.OutboxEventEntity;
import com.company.companyservice.infrastructure.persistence.command.OutboxRelayRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxRelayScheduler {

    private static final int BATCH_SIZE = 50;

    private final OutboxRelayRepository outboxRepository;
    private final OutboxRelayService relayService;

    public OutboxRelayScheduler(OutboxRelayRepository outboxRepository, OutboxRelayService relayService) {
        this.outboxRepository = outboxRepository;
        this.relayService = relayService;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:5000}")
    public void relay() {
        List<OutboxEventEntity> pending = outboxRepository.findPending(BATCH_SIZE);
        for (OutboxEventEntity entry : pending) {
            relayService.processOne(entry.getId());
        }
    }
}
